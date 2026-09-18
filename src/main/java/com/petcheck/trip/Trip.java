package com.petcheck.trip;

import com.petcheck.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "trip")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	public Trip(String title, LocalDate startDate, LocalDate endDate, Long memberId) {
		this.title = title;
		this.startDate = startDate;
		this.endDate = endDate;
		this.memberId = memberId;
	}

	public void updateDates(String title, LocalDate startDate, LocalDate endDate) {
		this.title = title;
		this.startDate = startDate;
		this.endDate = endDate;
	}
}
