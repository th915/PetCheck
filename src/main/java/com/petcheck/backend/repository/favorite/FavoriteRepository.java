package com.petcheck.backend.repository.favorite;

import com.petcheck.backend.entity.favorite.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMemberIdAndPlaceId(Long memberId, Long placeId);

    void deleteByMemberIdAndPlaceId(Long memberId, Long placeId);

    void deleteByMemberId(Long memberId);

    /**
     * 신뢰도 등급이 바뀐 장소들의 알림 수신자를 한 번에 조회한다.
     * 장소마다 따로 물으면 배치가 장소 수만큼 쿼리를 낸다.
     */
    @Query("select distinct f.member.id from Favorite f where f.place.id in :placeIds")
    List<Long> findMemberIdsByPlaceIds(@Param("placeIds") Collection<Long> placeIds);

    @Query("select f.place.id from Favorite f where f.member.id = :memberId and f.place.id in :placeIds")
    List<Long> findFavoritedPlaceIdsRaw(@Param("memberId") Long memberId, @Param("placeIds") Collection<Long> placeIds);

    @Query("select f.place.id from Favorite f where f.member.id = :memberId")
    List<Long> findAllPlaceIdsByMemberId(@Param("memberId") Long memberId);
}
