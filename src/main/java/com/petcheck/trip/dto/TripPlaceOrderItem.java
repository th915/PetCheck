package com.petcheck.trip.dto;

import jakarta.validation.constraints.NotNull;

public record TripPlaceOrderItem(@NotNull Long tripPlaceId, @NotNull Integer sortOrder) {
}
