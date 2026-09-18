package com.petcheck.trip;

import com.petcheck.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "trip_place")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlace extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "visit_date", nullable = false)
	private LocalDate visitDate;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Column(name = "reservation_time")
	private LocalTime reservationTime;

	@Column(name = "trip_id", nullable = false)
	private Long tripId;

	@Column(name = "place_id", nullable = false)
	private Long placeId;

	public TripPlace(LocalDate visitDate, Integer sortOrder, LocalTime reservationTime, Long tripId, Long placeId) {
		this.visitDate = visitDate;
		this.sortOrder = sortOrder;
		this.reservationTime = reservationTime;
		this.tripId = tripId;
		this.placeId = placeId;
	}

	public void changeVisitDate(LocalDate visitDate) {
		this.visitDate = visitDate;
	}

	public void changeSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void changeReservationTime(LocalTime reservationTime) {
		this.reservationTime = reservationTime;
	}

	public void shiftVisitDate(long days) {
		this.visitDate = this.visitDate.plusDays(days);
	}
}
