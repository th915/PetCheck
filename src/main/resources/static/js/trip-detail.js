(function () {
	let trip = null;
	let selectedDate = null;

	const dateTabsEl = document.getElementById("dateTabs");
	const listEl = document.getElementById("tripPlaceList");
	const titleViewEl = document.getElementById("tripViewTitle");
	const datesViewEl = document.getElementById("tripViewDates");
	const editDatesForm = document.getElementById("editDatesForm");
	const editDatesInputs = document.getElementById("editDatesInputs");
	const editTitleInput = document.getElementById("editTitle");
	const editStartInput = document.getElementById("editStartDate");
	const editEndInput = document.getElementById("editEndDate");

	function currentDates() {
		return TripCommon.dateRange(trip.startDate, trip.endDate);
	}

	function mapItem(p) {
		return {
			key: String(p.tripPlaceId),
			tripPlaceId: p.tripPlaceId,
			placeId: p.placeId,
			name: p.placeName,
			latitude: p.latitude,
			longitude: p.longitude,
			visitDate: p.visitDate,
			sortOrder: p.sortOrder,
			reservationTime: p.reservationTime,
			favorited: p.favorited,
		};
	}

	function renderMap() {
		MapKit.clearAll();
		const items = trip.places.map(mapItem);
		const daySorted = items.filter((p) => p.visitDate === selectedDate).sort((a, b) => a.sortOrder - b.sortOrder);
		const orderIndex = new Map(daySorted.map((p, i) => [p.key, i + 1]));
		items.forEach((p) => {
			const label = orderIndex.has(p.key) ? String(orderIndex.get(p.key)) : null;
			MapKit.addMarker(p.latitude, p.longitude, label, () => PlacePopup.open(p.placeId));
		});
		MapKit.drawPolyline(daySorted.map((p) => ({ lat: p.latitude, lng: p.longitude })), "#d64545");
		if (items.length) MapKit.fitBounds(items.map((p) => ({ lat: p.latitude, lng: p.longitude })));
	}

	function selectDate(d) {
		selectedDate = d;
		renderAll();
	}

	const handlers = {
		onDateChange: async (tripPlaceId, newDate) => {
			try {
				await Api.patch(`/api/trips/${trip.id}/places/${tripPlaceId}/date`, { visitDate: newDate });
				await reload();
			} catch (e) {
				alert(e.message);
			}
		},
		onTimeChange: async (tripPlaceId, value) => {
			try {
				await Api.patch(`/api/trips/${trip.id}/places/${tripPlaceId}/reservation`, { reservationTime: value || null });
				await reload();
			} catch (e) {
				alert(e.message);
			}
		},
		onRemove: async (tripPlaceId) => {
			if (!confirm("이 장소를 일정에서 제거하시겠습니까?")) return;
			try {
				await Api.del(`/api/trips/${trip.id}/places/${tripPlaceId}`);
				await reload();
			} catch (e) {
				alert(e.message);
			}
		},
		onReorder: async (date, orderedKeys) => {
			try {
				const items = orderedKeys.map((k, i) => ({ tripPlaceId: Number(k), sortOrder: i }));
				await Api.patch(`/api/trips/${trip.id}/places/reorder`, items);
				await reload();
			} catch (e) {
				alert(e.message);
			}
		},
	};

	function renderAll() {
		titleViewEl.textContent = trip.title;
		datesViewEl.textContent = `${trip.startDate} ~ ${trip.endDate}`;
		const dates = currentDates();
		if (!dates.includes(selectedDate)) selectedDate = trip.startDate;
		TripCommon.renderDateTabs(dateTabsEl, dates, selectedDate, selectDate);

		const items = trip.places.map(mapItem).filter((p) => p.visitDate === selectedDate);
		TripCommon.renderPlaceList(listEl, items, [selectedDate], handlers, { showFavorite: true, selectDates: dates });
		renderMap();
	}

	async function reload() {
		trip = await Api.get(`/api/trips/${TRIP_ID}`);
		renderAll();
	}

	document.getElementById("editDatesBtn").addEventListener("click", () => {
		const opening = editDatesForm.style.display === "none";
		editDatesForm.style.display = opening ? "flex" : "none";
		editDatesInputs.style.display = opening ? "flex" : "none";
		if (opening) {
			editTitleInput.value = trip.title;
			editStartInput.value = trip.startDate;
			editEndInput.value = trip.endDate;
		}
	});

	document.getElementById("saveDatesBtn").addEventListener("click", async () => {
		const title = editTitleInput.value.trim();
		const startDate = editStartInput.value;
		const endDate = editEndInput.value;
		if (!title || !startDate || !endDate) {
			alert("여행 이름과 날짜를 모두 입력해주세요.");
			return;
		}
		if (endDate < startDate) {
			alert("종료일은 시작일보다 빠를 수 없습니다.");
			return;
		}
		if (!confirm("여행 날짜를 변경하면 모든 장소의 방문일이 첫날로 초기화됩니다. 계속할까요?")) return;
		try {
			trip = await Api.patch(`/api/trips/${trip.id}`, { title, startDate, endDate });
			selectedDate = trip.startDate;
			editDatesForm.style.display = "none";
			editDatesInputs.style.display = "none";
			renderAll();
		} catch (e) {
			alert(e.message);
		}
	});

	document.getElementById("deleteTripBtn").addEventListener("click", async () => {
		if (!confirm("이 여행 일정을 삭제하시겠습니까? 되돌릴 수 없습니다.")) return;
		try {
			await Api.del(`/api/trips/${TRIP_ID}`);
			location.href = "/trips";
		} catch (e) {
			alert(e.message);
		}
	});

	PlacePopup.onFavoriteToggled = (placeId, favorited) => {
		if (!trip) return;
		trip.places.filter((p) => String(p.placeId) === String(placeId)).forEach((p) => (p.favorited = favorited));
		renderAll();
	};

	(async () => {
		await MapKit.init("map", KAKAO_JS_KEY);
		await reload();
	})();
})();
