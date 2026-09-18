package com.petcheck.trip;

import com.petcheck.backend.common.security.MemberPrincipal;
import com.petcheck.trip.dto.TripCreateRequest;
import com.petcheck.trip.dto.TripDetailResponse;
import com.petcheck.trip.dto.TripPlaceDateRequest;
import com.petcheck.trip.dto.TripPlaceOrderItem;
import com.petcheck.trip.dto.TripPlaceReservationRequest;
import com.petcheck.trip.dto.TripSummaryResponse;
import com.petcheck.trip.dto.TripUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripApiController {

	private final TripService tripService;

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public Map<String, Long> create(@Valid @RequestBody TripCreateRequest request,
			@AuthenticationPrincipal MemberPrincipal principal) {
		Long id = tripService.create(principal.id(), request);
		return Map.of("id", id);
	}

	@GetMapping
	public List<TripSummaryResponse> list(@AuthenticationPrincipal MemberPrincipal principal) {
		return tripService.list(principal.id());
	}

	@GetMapping("/{tripId}")
	public TripDetailResponse detail(@PathVariable Long tripId,
			@AuthenticationPrincipal MemberPrincipal principal) {
		return tripService.getDetail(tripId, principal.id());
	}

	@PatchMapping("/{tripId}")
	public TripDetailResponse update(@PathVariable Long tripId,
			@Valid @RequestBody TripUpdateRequest request,
			@AuthenticationPrincipal MemberPrincipal principal) {
		return tripService.updateDates(tripId, principal.id(), request);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{tripId}")
	public void delete(@PathVariable Long tripId, @AuthenticationPrincipal MemberPrincipal principal) {
		tripService.delete(tripId, principal.id());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{tripId}/places/{tripPlaceId}")
	public void removePlace(@PathVariable Long tripId, @PathVariable Long tripPlaceId,
			@AuthenticationPrincipal MemberPrincipal principal) {
		tripService.removePlace(tripId, principal.id(), tripPlaceId);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PatchMapping("/{tripId}/places/{tripPlaceId}/date")
	public void changeDate(@PathVariable Long tripId, @PathVariable Long tripPlaceId,
			@Valid @RequestBody TripPlaceDateRequest request,
			@AuthenticationPrincipal MemberPrincipal principal) {
		tripService.changePlaceDate(tripId, principal.id(), tripPlaceId, request.visitDate());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PatchMapping("/{tripId}/places/{tripPlaceId}/reservation")
	public void changeReservation(@PathVariable Long tripId, @PathVariable Long tripPlaceId,
			@RequestBody TripPlaceReservationRequest request,
			@AuthenticationPrincipal MemberPrincipal principal) {
		tripService.changePlaceReservation(tripId, principal.id(), tripPlaceId, request.reservationTime());
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PatchMapping("/{tripId}/places/reorder")
	public void reorder(@PathVariable Long tripId, @RequestBody List<TripPlaceOrderItem> items,
			@AuthenticationPrincipal MemberPrincipal principal) {
		tripService.reorder(tripId, principal.id(), items);
	}
}
