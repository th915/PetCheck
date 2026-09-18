package com.petcheck.trip.dto;

import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.PlaceCategory;
import com.petcheck.trip.TripPlace;
import java.time.LocalDate;
import java.time.LocalTime;

public record TripPlaceResponse(
		Long tripPlaceId,
		Long placeId,
		String placeName,
		String placeAddress,
		PlaceCategory category,
		Double latitude,
		Double longitude,
		LocalDate visitDate,
		Integer sortOrder,
		LocalTime reservationTime,
		boolean favorited
) {

	public static TripPlaceResponse of(TripPlace tripPlace, Place place, boolean favorited) {
		return new TripPlaceResponse(
				tripPlace.getId(),
				place.getId(),
				place.getName(),
				place.getAddress(),
				place.getCategory(),
				place.getLatitude(),
				place.getLongitude(),
				tripPlace.getVisitDate(),
				tripPlace.getSortOrder(),
				tripPlace.getReservationTime(),
				favorited
		);
	}
}
