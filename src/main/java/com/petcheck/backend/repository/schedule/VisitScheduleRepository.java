package com.petcheck.backend.repository.schedule;

import com.petcheck.backend.entity.schedule.VisitSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitScheduleRepository extends JpaRepository<VisitSchedule, Long> {

    void deleteByMemberId(Long memberId);

    /** 캘린더 화면 — 이번 달치를 한 번에 가져온다. */
    @EntityGraph(attributePaths = {"place", "petProfile"})
    List<VisitSchedule> findAllByMemberIdAndVisitAtBetween(
            Long memberId, LocalDateTime from, LocalDateTime to);

    /** 리마인드 배치 — 아직 안 보냈고 방문일이 코앞인 일정. */
    @EntityGraph(attributePaths = {"place", "member"})
    List<VisitSchedule> findAllByRemindedFalseAndVisitAtBetween(
            LocalDateTime from, LocalDateTime to);

    /**
     * 방문 전에 등록해둔 일정이 있는지. SCHEDULE 인증의 근거다.
     * <p>
     * createdAt < visitAt 조건이 핵심이다. 이게 없으면 다녀온 뒤에 일정을 만들어
     * 인증을 받는 사후 조작이 가능하다.
     */
    @Query("""
            select count(s) > 0
            from VisitSchedule s
            where s.member.id = :memberId
              and s.place.id = :placeId
              and s.visitAt between :from and :to
              and s.createdAt < s.visitAt
            """)
    boolean existsPriorSchedule(@Param("memberId") Long memberId,
                                @Param("placeId") Long placeId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}
