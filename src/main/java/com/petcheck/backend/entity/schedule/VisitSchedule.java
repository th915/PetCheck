package com.petcheck.backend.entity.schedule;

import com.petcheck.backend.entity.common.BaseTimeEntity;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.pet.PetProfile;
import com.petcheck.backend.entity.place.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 캘린더(프론트 메인 화면)의 일정 한 건.
 * 예약이 아니라 "내가 가려고 적어둔 계획"이다. 매장은 이 데이터를 보지 않는다.
 * 방문일이 다가오면 준비물과 법정기준 인증 여부를 리마인드한다.
 */
@Getter
@Entity
@Table(
        name = "visit_schedule",
        indexes = {
                @Index(name = "idx_schedule_member_date", columnList = "member_id, visit_at"),
                @Index(name = "idx_schedule_reminder", columnList = "reminded, visit_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /** 데리고 갈 아이. null 이면 특정하지 않은 일정이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_profile_id")
    private PetProfile petProfile;

    @Column(name = "visit_at", nullable = false)
    private LocalDateTime visitAt;

    @Column(length = 300)
    private String memo;

    /** 리마인드 발송 여부. 배치가 중복 발송하지 않도록 한 번만 세운다. */
    @Column(nullable = false)
    private boolean reminded;

    @Builder
    public VisitSchedule(Member member, Place place, PetProfile petProfile,
                         LocalDateTime visitAt, String memo) {
        this.member = member;
        this.place = place;
        this.petProfile = petProfile;
        this.visitAt = visitAt;
        this.memo = memo;
        this.reminded = false;
    }

    public void reschedule(LocalDateTime visitAt, String memo) {
        this.visitAt = visitAt;
        this.memo = memo;
        this.reminded = false;
    }

    public void markReminded() {
        this.reminded = true;
    }
}
