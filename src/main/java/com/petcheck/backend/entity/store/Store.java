package com.petcheck.backend.entity.store;

import com.petcheck.backend.entity.common.BaseTimeEntity;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.place.Place;
import jakarta.persistence.Column;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 매장이 직접 등록한 레이어. Place 위에 얹히며, Place 하나당 최대 하나다.
 * <p>
 * Place 와 합치지 않은 이유: 공공데이터만 있는 장소와 매장이 등록한 장소를
 * 리뷰 일치율로 비교하는 것이 제안서의 "출처별 정확도 비교" 지표다.
 * 테이블이 갈려 있으면 그 비교가 LEFT JOIN ... IS NULL 하나로 끝난다.
 */
@Getter
@Entity
@Table(
        name = "store",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_place", columnNames = "place_id"),
        indexes = {
                @Index(name = "idx_store_status", columnList = "verification_status"),
                @Index(name = "idx_store_certified", columnList = "legal_certified")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /** 등록한 사장님 계정. role 이 STORE 인 Member 만 올 수 있다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    /** 사업자등록번호. 데모 범위에서는 형식 검증과 관리자 수동 승인까지만 한다. */
    @Column(name = "business_number", length = 20)
    private String businessNumber;

    /** 법정 시설기준 체크리스트. MySQL JSON 컬럼. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "legal_checklist", columnDefinition = "json")
    private LegalChecklist legalChecklist;

    /** 증빙 이미지 경로. LocalImageStorage 가 돌려준 "/uploads/{uuid}.jpg" 가 들어간다. */
    @Column(name = "proof_image_url", length = 500)
    private String proofImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    /**
     * "승인됨 + 전 항목 충족"을 미리 계산해 둔 컬럼.
     * <p>
     * 체크리스트가 JSON 이라 SQL 에서 8개 항목을 뒤질 수 없다.
     * 법정기준 인증 비율 통계와 "인증 매장만" 필터가 이 한 칸을 읽는다.
     * 상태나 체크리스트가 바뀌는 모든 경로에서 syncCertification() 으로 함께 갱신된다.
     */
    @Column(name = "legal_certified", nullable = false)
    private boolean legalCertified;

    @Column(name = "reject_reason", length = 300)
    private String rejectReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Builder
    public Store(Place place, Member owner, String businessName, String businessNumber,
                 LegalChecklist legalChecklist, String proofImageUrl) {
        this.place = place;
        this.owner = owner;
        this.businessName = businessName;
        this.businessNumber = businessNumber;
        this.legalChecklist = legalChecklist;
        this.proofImageUrl = proofImageUrl;
        this.verificationStatus = VerificationStatus.PENDING;
        syncCertification();
    }

    public void approve() {
        this.verificationStatus = VerificationStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.rejectReason = null;
        syncCertification();
    }

    public void reject(String reason) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.rejectReason = reason;
        this.approvedAt = null;
        syncCertification();
    }

    /**
     * 체크리스트를 고치면 승인이 풀린다.
     * 승인 이후 항목만 몰래 바꿔 배지를 유지하는 것을 막는다.
     */
    public void updateChecklist(LegalChecklist legalChecklist, String proofImageUrl) {
        this.legalChecklist = legalChecklist;
        this.proofImageUrl = proofImageUrl;
        this.verificationStatus = VerificationStatus.PENDING;
        this.approvedAt = null;
        syncCertification();
    }

    /**
     * "법정 기준 인증" 배지 노출 조건이자 Place 사전 신뢰도를 0.8로 올리는 조건.
     * 관리자 승인과 전 항목 충족을 모두 만족해야 한다.
     */
    public boolean isLegalCertified() {
        return legalCertified;
    }

    private void syncCertification() {
        this.legalCertified = verificationStatus == VerificationStatus.APPROVED
                && legalChecklist != null
                && legalChecklist.isFullySatisfied();
    }
}
