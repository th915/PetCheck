package com.petcheck.backend.entity.place;

import com.petcheck.backend.entity.common.BaseTimeEntity;
import com.petcheck.backend.entity.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 장소 하나. 공공데이터 4종이 전부 이 모양으로 정규화되어 들어온다.
 * 매장이 직접 등록하면 Store 가 이 위에 얹히지만, Store 없이 Place 만 존재할 수 있다.
 * 그 차이가 곧 "출처별 정확도 비교" 지표의 두 집단이다.
 * <p>
 * 신뢰도 점수를 Place 가 직접 들고 있는 이유: 리뷰를 매번 집계하면
 * 지도에 100개 장소를 그릴 때마다 집계 쿼리가 100번 나간다.
 * 누적값을 갱신해두고 배치에서 점수만 다시 계산한다.
 */
@Getter
@Entity
@Table(
        name = "place",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_place_source",
                columnNames = {"source_type", "external_id"}
        ),
        indexes = {
                @Index(name = "idx_place_category", columnList = "category"),
                @Index(name = "idx_place_coords", columnList = "latitude, longitude"),
                @Index(name = "idx_place_trust_score", columnList = "trust_score")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- 출처 ---

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    /** 원본 기관의 식별자(TourAPI contentId 등). 재수집 시 중복 삽입을 막는다. */
    @Column(name = "external_id", length = 100)
    private String externalId;

    /**
     * 식품안전나라 목록과 대조해 "지자체 공식 등록"이 확인된 장소인지.
     * 사전 신뢰도를 한 단계 올리는 근거가 된다.
     */
    @Column(name = "publicly_registered", nullable = false)
    private boolean publiclyRegistered;

    // --- 기본 정보 ---

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceCategory category;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(length = 20)
    private String phone;

    /** 지도 반경 검색에 쓰므로 문자열이 아니라 수치로 저장한다. */
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // --- 동반 조건 ---

    @Embedded
    private PetPolicy petPolicy;

    // --- 매장 등록 레이어 (없을 수 있다) ---

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "place")
    private Store store;

    // --- 신뢰도 ---

    @Column(name = "trust_score", nullable = false)
    private double trustScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_grade", nullable = false, length = 20)
    private TrustGrade trustGrade;

    /** 누적 리뷰 수. 화면 표시용이다 — "12명 중 10명이 일치했다고 답했습니다" */
    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "matched_review_count", nullable = false)
    private int matchedReviewCount;

    /**
     * Σ (decayBaseᵢ × aᵢ). 시간과 무관한 누적값이라 리뷰가 추가·수정·삭제될 때만 바뀐다.
     * 조회 시점 t 의 실제 가중합은 여기에 2^(−t/H) 를 곱해서 얻는다.
     * 이 분리 덕분에 리뷰가 몇만 건이 되어도 갱신이 O(1) 이다.
     */
    @Column(name = "decay_sum", nullable = false)
    private double decaySum;

    /** Σ (decayBaseᵢ × aᵢ × matchᵢ) */
    @Column(name = "decay_match_sum", nullable = false)
    private double decayMatchSum;

    /** 최근 검증일 — 매장 상세 화면에 그대로 노출된다 */
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Builder
    public Place(SourceType sourceType, String externalId, boolean publiclyRegistered,
                 String name, PlaceCategory category, String address, String phone,
                 Double latitude, Double longitude, String imageUrl, PetPolicy petPolicy) {
        this.sourceType = sourceType;
        this.externalId = externalId;
        this.publiclyRegistered = publiclyRegistered;
        this.name = name;
        this.category = category;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.petPolicy = petPolicy;

        this.trustScore = priorScore();
        this.trustGrade = TrustGrade.of(this.trustScore);
    }

    /**
     * 공공데이터 재수집 시 호출. 이름·주소·좌표는 갱신하되
     * 신뢰도 누적값은 건드리지 않는다. 리뷰로 쌓은 검증 결과를 날리면 안 된다.
     * <p>
     * 동반 조건은 매장 등록이 없을 때만 덮는다.
     * 매장이 직접 입력한 값이 공공데이터보다 최신이고, 그게 매장 등록의 존재 이유다.
     * 이 가드가 없으면 재동기화 한 번에 매장이 입력한 조건이 전부 사라진다.
     */
    public void refreshFromPublicData(String name, PlaceCategory category,
                                      String address, String phone,
                                      Double latitude, Double longitude, PetPolicy petPolicy) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;

        if (store == null) {
            this.petPolicy = petPolicy;
        }
    }

    public void markPubliclyRegistered() {
        this.publiclyRegistered = true;
    }

    /** 매장이 입력한 동반 조건으로 교체한다. 공공데이터보다 우선한다. */
    public void applyStorePolicy(PetPolicy petPolicy) {
        this.petPolicy = petPolicy;
    }

    /**
     * 연관관계 편의 메서드.
     * <p>
     * store 는 mappedBy 쪽이라 Store 를 저장해도 이 필드는 자동으로 채워지지 않는다.
     * priorScore() 가 store 를 읽으므로, 등록·승인 직후 신뢰도를 다시 계산하려면
     * 같은 트랜잭션 안에서 직접 연결해줘야 한다.
     */
    public void attachStore(Store store) {
        this.store = store;
    }

    /**
     * 저장된 누적값에 감쇠 계수를 곱해 점수를 다시 계산한다.
     * <pre>
     *   trust_score = (C × m + decayMatchSum × factor) / (C + decaySum × factor)
     * </pre>
     * 리뷰 테이블을 읽지 않는다. factor 는 호출자가 현재 시각으로 계산해 넘긴다.
     *
     * @param weight 사전 확률 가중치 C
     * @param factor 2^(−경과일/반감기)
     * @return 재계산 이전 등급. 등급이 바뀌면 알림 대상이 된다
     */
    public TrustGrade refreshTrustScore(double weight, double factor) {
        TrustGrade previousGrade = this.trustGrade;

        double weightedSum = decaySum * factor;
        double weightedMatchSum = decayMatchSum * factor;

        this.trustScore = (weight * priorScore() + weightedMatchSum) / (weight + weightedSum);
        this.trustGrade = TrustGrade.of(this.trustScore);

        return previousGrade;
    }

    /** 리뷰가 달릴 때 최근 검증일을 앞당긴다. 누적 합은 리포지토리가 원자적으로 더한다 */
    public void markVerified(LocalDateTime visitedAt) {
        if (lastVerifiedAt == null || visitedAt.isAfter(lastVerifiedAt)) {
            this.lastVerifiedAt = visitedAt;
        }
    }

    /**
     * 사전 신뢰도 m. 출처와 검증 단계에 따라 세 구간으로 나뉜다.
     * 공식에서 가장 논쟁적인 부분이라 한곳에 모아둔다.
     */
    private double priorScore() {
        if (store != null && store.isLegalCertified()) {
            return 0.8;
        }
        if (publiclyRegistered) {
            return 0.65;
        }
        return 0.5;
    }
}
