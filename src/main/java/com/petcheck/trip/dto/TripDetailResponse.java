package com.petcheck.trip.dto;

import com.petcheck.trip.Trip;
import java.time.LocalDate;
import java.util.List;

public record TripDetailResponse(
		Long id,
		String title,
		LocalDate startDate,
		LocalDate endDate,
		List<TripPlaceResponse> places
) {

	public static TripDetailResponse of(Trip trip, List<TripPlaceResponse> places) {
		return new TripDetailResponse(trip.getId(), trip.getTitle(), trip.getStartDate(), trip.getEndDate(), places);
	}
}
