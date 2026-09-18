package com.petcheck.trip.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record TripPlaceCreateRequest(
		@NotNull Long placeId,
		@NotNull LocalDate visitDate,
		@NotNull Integer sortOrder,
		LocalTime reservationTime
) {
}
