package com.petcheck.backend.repository.pet;

import com.petcheck.backend.entity.pet.PetProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetProfileRepository extends JpaRepository<PetProfile, Long> {

    List<PetProfile> findAllByMemberId(Long memberId);

    long countByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
