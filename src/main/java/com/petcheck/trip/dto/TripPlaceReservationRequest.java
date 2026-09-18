package com.petcheck.trip.dto;

import java.time.LocalTime;

public record TripPlaceReservationRequest(LocalTime reservationTime) {
}
