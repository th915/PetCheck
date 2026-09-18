package com.petcheck.backend.repository.review;

import com.petcheck.backend.dto.review.MismatchReasonCount;
import com.petcheck.backend.entity.review.AuthenticityStatus;
import com.petcheck.backend.entity.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 매장 상세 화면의 리뷰 목록. 작성자 닉네임을 찍어야 하므로 member 를 같이 가져온다. */
    @EntityGraph(attributePaths = {"member"})
    Page<Review> findAllByPlaceIdOrderByCreatedAtDesc(Long placeId, Pageable pageable);

    /** 매장 상세의 리뷰 목록. FAKE 는 내려주지 않는다 */
    @EntityGraph(attributePaths = {"member"})
    Page<Review> findAllByPlaceIdAndAuthenticityStatusNotOrderByCreatedAtDesc(
            Long placeId, AuthenticityStatus excluded, Pageable pageable);

    /** 내가 쓴 리뷰 */
    @EntityGraph(attributePaths = {"place"})
    Page<Review> findAllByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /** 같은 장소·같은 방문일에 이미 썼는지 */
    boolean existsByPlaceIdAndMemberIdAndVisitedAt(Long placeId, Long memberId, LocalDateTime visitedAt);
    /**
     * 탈퇴 시 정리용. 벌크 삭제(deleteByMemberId)를 쓰지 않는 이유는
     * mismatchReasons 가 @ElementCollection 이라 별도 테이블 행이 남아 외래키에 걸리기 때문이다.
     */
    List<Review> findAllByMemberId(Long memberId);

    /**
     * "어떤 정보가 가장 자주 틀리는가" 집계.
     * <p>
     * mismatchReasons 가 @ElementCollection 이라 조인 대상이 되고,
     * 별칭 reason 이 곧 열거형 값이므로 그대로 group by 할 수 있다.
     * 이 값을 JSON 배열로 저장했다면 이 쿼리는 SQL 로 짤 수 없었다.
     */
    @Query("""
            select new com.petcheck.backend.dto.review.MismatchReasonCount(reason, count(r))
            from Review r
            join r.mismatchReasons reason
            where r.createdAt >= :from
            group by reason
            order by count(r) desc
            """)
    List<MismatchReasonCount> countMismatchReasons(@Param("from") LocalDateTime from);

    /** 관리자 모니터링용 — 상태별 조회 */
    Page<Review> findAllByAuthenticityStatusOrderByCreatedAtDesc(AuthenticityStatus status, Pageable pageable);

    /** 도배 검사 — 최근 N분 안에 이 회원이 쓴 리뷰 수 */
    long countByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime from);

    /** 복붙 검사 — 이 회원이 같은 본문을 재사용했는지 */
    long countByMemberIdAndContent(Long memberId, String content);

    /** 다계정 검사 — 같은 단말(IP 해시)에서 같은 장소에 글을 쓴 서로 다른 계정 수 */
    @Query("""
            select count(distinct r.member.id) from Review r
            where r.writerIpHash = :ipHash
              and r.place.id = :placeId
              and r.createdAt > :from
            """)
    long countDistinctWritersByIp(@Param("ipHash") String ipHash,
                                  @Param("placeId") Long placeId,
                                  @Param("from") LocalDateTime from);

}
