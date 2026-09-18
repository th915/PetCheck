package com.petcheck.backend.repository.store;

import com.petcheck.backend.entity.store.Store;
import com.petcheck.backend.entity.store.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByPlaceId(Long placeId);

    boolean existsByPlaceId(Long placeId);
    boolean existsByBusinessNumber(String businessNumber);


    /**
     * 관리자 승인 대기 목록. place 와 owner 를 같이 끌어와야
     * 목록 화면에서 매장명·주소를 찍을 때 N+1 이 나지 않는다.
     */
    @EntityGraph(attributePaths = {"place", "owner"})
    Page<Store> findAllByVerificationStatus(VerificationStatus status, Pageable pageable);

    /** 사장님의 내 매장 조회. 상세 화면에 장소명을 찍어야 하므로 place 를 같이 가져온다 */

    /** 사장님의 매장 목록. 여러 개일 수 있다 */
    @EntityGraph(attributePaths = {"place"})
    List<Store> findAllByOwnerId(Long ownerId);

    long countByLegalCertifiedTrue();
}
