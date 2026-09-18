package com.petcheck.backend.entity.review;

import com.petcheck.backend.entity.common.BaseTimeEntity;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.place.Place;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 별점 리뷰가 아니다. 핵심 필드는 matched 하나 — "등록된 정보와 실제가 일치했는가".
 * <p>
 * decayBase 가 이 리뷰의 시간 독립 가중치다. v × 2^(방문일/반감기) 로 생성 시 한 번 계산하고
 * 이후 바뀌지 않는다(인증 등급이 올라갈 때만 재계산).
 * 실제 가중치는 조회 시점에 decayBase × 감쇠계수 × 진정성가중치 로 만들어진다.
 * 이 분리 덕분에 Place 가 합만 들고 있으면 되고, 리뷰를 다시 읽을 필요가 없다.
 */
@Getter
@Entity
@Table(
        name = "review",
        indexes = {
                @Index(name = "idx_review_place_created", columnList = "place_id, created_at"),
                @Index(name = "idx_review_member_created", columnList = "member_id, created_at"),
                @Index(name = "idx_review_status", columnList = "authenticity_status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 공식의 match. true = 1, false = 0 */
    @Column(nullable = false)
    private boolean matched;

    /**
     * matched 가 false 일 때만 채워진다.
     * JSON 이 아니라 별도 테이블인 이유는 "어떤 정보가 자주 틀리는가"를 GROUP BY 해야 하기 때문이다.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "review_mismatch_reason",
            joinColumns = @JoinColumn(name = "review_id"),
            indexes = @Index(name = "idx_rmr_reason", columnList = "reason")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private Set<MismatchReason> mismatchReasons = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_image", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url", nullable = false, length = 500)
    private List<String> imageUrls = new ArrayList<>();

    /** 방문일. 수정 불가 — 바꿀 수 있으면 오래된 방문을 최신으로 위조할 수 있다 */
    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    @Column(length = 1000)
    private String content;

    @Embedded
    private ReviewVerification verification;

    @Embedded
    private ReviewAuthenticity authenticity;

    /** v × 2^(방문일 − 기준일 / 반감기). 인증 등급이나 방문일이 바뀔 때만 다시 계산된다 */
    @Column(name = "decay_base", nullable = false)
    private double decayBase;

    /** 작성자 IP 의 SHA-256. 원본은 남기지 않고 동일 단말 도배 탐지에만 쓴다 */
    @Column(name = "writer_ip_hash", length = 64)
    private String writerIpHash;

    /** 영수증 사진 경로. 일반 사진(imageUrls)과 용도가 달라 따로 둔다 */
    private String receiptImageUrl;

    @Builder
    public Review(Place place, Member member, boolean matched,
                  Collection<MismatchReason> mismatchReasons, Collection<String> imageUrls,
                  LocalDateTime visitedAt, String content,
                  ReviewVerification verification, double decayBase, String writerIpHash,
                  String receiptImageUrl) {

        this.place = place;
        this.member = member;
        this.matched = matched;
        this.mismatchReasons = matched || mismatchReasons == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(mismatchReasons);
        this.imageUrls = imageUrls == null ? new ArrayList<>() : new ArrayList<>(imageUrls);
        this.visitedAt = visitedAt;
        this.content = content;
        this.verification = verification;
        this.authenticity = ReviewAuthenticity.pending();
        this.decayBase = decayBase;
        this.writerIpHash = writerIpHash;
        this.receiptImageUrl = receiptImageUrl;
    }

    /**
     * 본문·일치 여부·사진을 고친다. 방문일과 인증 등급은 건드리지 않는다.
     * 본문이 바뀌었으면 진정성 판정을 되돌려 재분석 대상으로 만든다.
     */
    public void update(boolean matched, Collection<MismatchReason> mismatchReasons,
                       Collection<String> imageUrls, String content) {

        boolean contentChanged = !java.util.Objects.equals(this.content, content);

        this.matched = matched;
        this.mismatchReasons = matched || mismatchReasons == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(mismatchReasons);
        this.imageUrls = imageUrls == null ? new ArrayList<>() : new ArrayList<>(imageUrls);
        this.content = content;

        if (contentChanged) {
            this.authenticity = ReviewAuthenticity.pending();
        }
    }

    public void applyAuthenticity(ReviewAuthenticity authenticity) {
        this.authenticity = authenticity;
    }

    /** 영수증·EXIF 검증이 나중에 통과했을 때 등급을 올린다 */
    public void upgradeVerification(ReviewVerification verification, double decayBase) {
        this.verification = verification;
        this.decayBase = decayBase;
    }

    public boolean isOwnedBy(Long memberId) {
        return member.getId().equals(memberId);
    }

    /**
     * 영수증이 확인되면 인증 등급과 감쇠 기준값을 함께 올린다.
     * decayBase 는 v × 2^(t/H) 라서 v 가 바뀌면 같이 다시 계산해야 한다.
     */
    public void applyReceipt(ReviewVerification verification, double decayBase) {
        this.verification = verification;
        this.decayBase = decayBase;
    }
}