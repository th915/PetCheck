package com.petcheck.trip;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripPlaceRepository extends JpaRepository<TripPlace, Long> {

	List<TripPlace> findByTripIdOrderByVisitDateAscSortOrderAsc(Long tripId);

	Optional<TripPlace> findByIdAndTripId(Long id, Long tripId);

	int countByTripIdAndVisitDate(Long tripId, java.time.LocalDate visitDate);
}
