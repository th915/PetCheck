package com.petcheck.trip;

import com.petcheck.backend.common.exception.BusinessException;
import com.petcheck.backend.common.exception.ErrorCode;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.repository.favorite.FavoriteRepository;
import com.petcheck.backend.repository.place.PlaceRepository;
import com.petcheck.trip.dto.TripCreateRequest;
import com.petcheck.trip.dto.TripDetailResponse;
import com.petcheck.trip.dto.TripPlaceCreateRequest;
import com.petcheck.trip.dto.TripPlaceOrderItem;
import com.petcheck.trip.dto.TripPlaceResponse;
import com.petcheck.trip.dto.TripSummaryResponse;
import com.petcheck.trip.dto.TripUpdateRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

	private final TripRepository tripRepository;
	private final TripPlaceRepository tripPlaceRepository;
	private final PlaceRepository placeRepository;
	private final FavoriteRepository favoriteRepository;

	@Transactional
	public Long create(Long memberId, TripCreateRequest request) {
		if (request.endDate().isBefore(request.startDate())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
		}
		Trip trip = tripRepository.save(new Trip(request.title(), request.startDate(), request.endDate(), memberId));

		List<TripPlaceCreateRequest> places = request.places() == null ? List.of() : request.places();
		for (TripPlaceCreateRequest p : places) {
			if (p.visitDate().isBefore(request.startDate()) || p.visitDate().isAfter(request.endDate())) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, "방문 날짜는 여행 기간 내여야 합니다.");
			}
			tripPlaceRepository.save(new TripPlace(p.visitDate(), p.sortOrder(), p.reservationTime(), trip.getId(), p.placeId()));
		}
		return trip.getId();
	}

	public List<TripSummaryResponse> list(Long memberId) {
		return tripRepository.findByMemberIdOrderByStartDateDesc(memberId).stream()
				.map(TripSummaryResponse::from)
				.toList();
	}

	public TripDetailResponse getDetail(Long tripId, Long memberId) {
		Trip trip = getOwnedTrip(tripId, memberId);
		return TripDetailResponse.of(trip, buildPlaceResponses(trip.getId(), memberId));
	}

	@Transactional
	public TripDetailResponse updateDates(Long tripId, Long memberId, TripUpdateRequest request) {
		if (request.endDate().isBefore(request.startDate())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
		}
		Trip trip = getOwnedTrip(tripId, memberId);
		trip.updateDates(request.title(), request.startDate(), request.endDate());

		List<TripPlace> tripPlaces = tripPlaceRepository.findByTripIdOrderByVisitDateAscSortOrderAsc(tripId);
		for (TripPlace tp : tripPlaces) {
			tp.changeVisitDate(request.startDate());
		}
		return TripDetailResponse.of(trip, buildPlaceResponses(tripId, memberId));
	}

	@Transactional
	public void delete(Long tripId, Long memberId) {
		Trip trip = getOwnedTrip(tripId, memberId);
		tripRepository.delete(trip);
	}

	@Transactional
	public void removePlace(Long tripId, Long memberId, Long tripPlaceId) {
		getOwnedTrip(tripId, memberId);
		TripPlace tripPlace = tripPlaceRepository.findByIdAndTripId(tripPlaceId, tripId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TRIP_PLACE_NOT_FOUND));
		tripPlaceRepository.delete(tripPlace);
	}

	@Transactional
	public void changePlaceDate(Long tripId, Long memberId, Long tripPlaceId, LocalDate visitDate) {
		Trip trip = getOwnedTrip(tripId, memberId);
		if (visitDate.isBefore(trip.getStartDate()) || visitDate.isAfter(trip.getEndDate())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "방문 날짜는 여행 기간 내여야 합니다.");
		}
		TripPlace tripPlace = tripPlaceRepository.findByIdAndTripId(tripPlaceId, tripId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TRIP_PLACE_NOT_FOUND));
		int countOnTargetDate = tripPlaceRepository.countByTripIdAndVisitDate(tripId, visitDate);
		tripPlace.changeVisitDate(visitDate);
		tripPlace.changeSortOrder(countOnTargetDate);
	}

	@Transactional
	public void changePlaceReservation(Long tripId, Long memberId, Long tripPlaceId, LocalTime reservationTime) {
		getOwnedTrip(tripId, memberId);
		TripPlace tripPlace = tripPlaceRepository.findByIdAndTripId(tripPlaceId, tripId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TRIP_PLACE_NOT_FOUND));
		tripPlace.changeReservationTime(reservationTime);
	}

	@Transactional
	public void reorder(Long tripId, Long memberId, List<TripPlaceOrderItem> items) {
		getOwnedTrip(tripId, memberId);
		for (TripPlaceOrderItem item : items) {
			tripPlaceRepository.findByIdAndTripId(item.tripPlaceId(), tripId)
					.ifPresent(tp -> tp.changeSortOrder(item.sortOrder()));
		}
	}

	private Trip getOwnedTrip(Long tripId, Long memberId) {
		return tripRepository.findByIdAndMemberId(tripId, memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
	}

	private List<TripPlaceResponse> buildPlaceResponses(Long tripId, Long memberId) {
		List<TripPlace> tripPlaces = tripPlaceRepository.findByTripIdOrderByVisitDateAscSortOrderAsc(tripId);
		if (tripPlaces.isEmpty()) {
			return List.of();
		}
		List<Long> placeIds = tripPlaces.stream().map(TripPlace::getPlaceId).distinct().toList();
		Map<Long, Place> placeMap = placeRepository.findAllById(placeIds).stream()
				.collect(java.util.stream.Collectors.toMap(Place::getId, Function.identity()));
		Set<Long> favoritedIds = memberId == null ? Set.of() : Set.copyOf(favoriteRepository.findFavoritedPlaceIdsRaw(memberId, placeIds));

		return tripPlaces.stream()
				.map(tp -> TripPlaceResponse.of(tp, placeMap.get(tp.getPlaceId()), favoritedIds.contains(tp.getPlaceId())))
				.toList();
	}
}
