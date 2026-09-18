package com.petcheck.backend.entity.trust;

import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.TrustGrade;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재계산 시점마다 남기는 스냅샷.
 * 최신값만 place 에 두면 "지난 30일간 얼마나 개선됐는지"를 보여줄 수 없다.
 * 정확도 대시보드의 추이 차트가 전부 이 테이블에서 나온다.
 * <p>
 * BaseTimeEntity 를 상속하지 않는다. 이 행은 수정되지 않고 append 만 되므로
 * updatedAt 이 의미가 없고, calculatedAt 하나가 곧 시간축이다.
 */
@Getter
@Entity
@Table(
        name = "trust_score_history",
        indexes = {
                @Index(name = "idx_tsh_place_time", columnList = "place_id, calculated_at"),
                @Index(name = "idx_tsh_changed", columnList = "grade_changed, calculated_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrustScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "trust_score", nullable = false)
    private double trustScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_grade", nullable = false, length = 20)
    private TrustGrade trustGrade;

    /** 스냅샷 시점의 누적 리뷰 수. 점수와 함께 봐야 표본 크기를 알 수 있다. */
    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    /** 이번 재계산으로 등급이 바뀌었는지. "리뷰로 재조정된 장소 수" 지표가 이 칸을 센다. */
    @Column(name = "grade_changed", nullable = false)
    private boolean gradeChanged;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Builder
    public TrustScoreHistory(Place place, boolean gradeChanged, LocalDateTime calculatedAt) {
        this.place = place;
        this.trustScore = place.getTrustScore();
        this.trustGrade = place.getTrustGrade();
        this.reviewCount = place.getReviewCount();
        this.gradeChanged = gradeChanged;
        this.calculatedAt = calculatedAt;
    }
}
