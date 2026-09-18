package com.petcheck.trip;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

	List<Trip> findByMemberIdOrderByStartDateDesc(Long memberId);

	Optional<Trip> findByIdAndMemberId(Long id, Long memberId);
}
