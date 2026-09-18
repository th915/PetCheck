package com.petcheck.trip.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TripPlaceDateRequest(@NotNull LocalDate visitDate) {
}
