package com.petcheck.trip.dto;

import com.petcheck.trip.Trip;
import java.time.LocalDate;

public record TripSummaryResponse(Long id, String title, LocalDate startDate, LocalDate endDate) {

	public static TripSummaryResponse from(Trip trip) {
		return new TripSummaryResponse(trip.getId(), trip.getTitle(), trip.getStartDate(), trip.getEndDate());
	}
}
