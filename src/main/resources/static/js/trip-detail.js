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
				UIDialog.alert(e.message);
			}
		},
		onTimeChange: async (tripPlaceId, value) => {
			try {
				await Api.patch(`/api/trips/${trip.id}/places/${tripPlaceId}/reservation`, { reservationTime: value || null });
				await reload();
			} catch (e) {
				UIDialog.alert(e.message);
			}
		},
		onRemove: async (tripPlaceId) => {
			if (!(await UIDialog.confirm("이 장소를 일정에서 제거하시겠습니까?"))) return;
			try {
				await Api.del(`/api/trips/${trip.id}/places/${tripPlaceId}`);
				await reload();
			} catch (e) {
				UIDialog.alert(e.message);
			}
		},
		onReorder: async (date, orderedKeys) => {
			try {
				const items = orderedKeys.map((k, i) => ({ tripPlaceId: Number(k), sortOrder: i }));
				await Api.patch(`/api/trips/${trip.id}/places/reorder`, items);
				await reload();
			} catch (e) {
				UIDialog.alert(e.message);
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
			UIDialog.alert("여행 이름과 날짜를 모두 입력해주세요.");
			return;
		}
		if (endDate < startDate) {
			UIDialog.alert("종료일은 시작일보다 빠를 수 없습니다.");
			return;
		}
		if (!(await UIDialog.confirm("여행 날짜를 변경하면 모든 장소의 방문일이 첫날로 초기화됩니다. 계속할까요?"))) return;
		try {
			trip = await Api.patch(`/api/trips/${trip.id}`, { title, startDate, endDate });
			selectedDate = trip.startDate;
			editDatesForm.style.display = "none";
			editDatesInputs.style.display = "none";
			renderAll();
		} catch (e) {
			UIDialog.alert(e.message);
		}
	});

	document.getElementById("deleteTripBtn").addEventListener("click", async () => {
		if (!(await UIDialog.confirm("이 여행 일정을 삭제하시겠습니까? 되돌릴 수 없습니다."))) return;
		try {
			await Api.del(`/api/trips/${TRIP_ID}`);
			location.href = "/trips";
		} catch (e) {
			UIDialog.alert(e.message);
		}
	});

	PlacePopup.onFavoriteToggled = (placeId, favorited) => {
		if (!trip) return;
		trip.places.filter((p) => String(p.placeId) === String(placeId)).forEach((p) => (p.favorited = favorited));
		renderAll();
	};

	initPanelResize();

	/**
	 * 화면이 좁으면 320px 고정폭인 왼쪽 일정 정보가 잘려 보이는 문제가 있었다.
	 * .app-shell 의 두 번째 그리드 트랙(일정 정보 패널) 폭을 드래그로 바꿔서
	 * 사용자가 직접 얼마나 넓게 볼지 고르게 한다. 폭은 새로고침해도 유지된다.
	 */
	function initPanelResize() {
		const handle = document.getElementById("panelResizeHandle");
		const shell = document.querySelector(".app-shell");
		if (!handle || !shell) return;

		const SIDE_NAV_WIDTH = 64;
		const MIN_WIDTH = 220;
		const MAX_WIDTH = Math.min(720, window.innerWidth - 320);

		/** 핸들을 app-shell 기준 절대좌표로 옮긴다 — search-panel 안이 아니라 밖에 있어서 직접 계산해야 한다 */
		function applyWidth(width) {
			shell.style.gridTemplateColumns = `${SIDE_NAV_WIDTH}px ${width}px 1fr 320px`;
			handle.style.left = `${SIDE_NAV_WIDTH + width - 5}px`;
		}

		let saved = null;
		try {
			saved = Number(localStorage.getItem("petcheck.tripPanelWidth"));
		} catch (e) {
			// 프라이빗 모드 등에서 접근이 막힐 수 있다 — 기본값으로 진행한다
		}
		applyWidth(saved && saved >= MIN_WIDTH && saved <= MAX_WIDTH ? saved : 300);

		let dragging = false;
		let startX = 0;
		let startWidth = 0;

		handle.addEventListener("mousedown", (e) => {
			dragging = true;
			startX = e.clientX;
			startWidth = document.getElementById("tripInfoPanel").getBoundingClientRect().width;
			handle.classList.add("dragging");
			document.body.style.userSelect = "none";
			e.preventDefault();
		});

		window.addEventListener("mousemove", (e) => {
			if (!dragging) return;
			const width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, startWidth + (e.clientX - startX)));
			applyWidth(width);
			MapKit.relayout();
		});

		window.addEventListener("mouseup", () => {
			if (!dragging) return;
			dragging = false;
			handle.classList.remove("dragging");
			document.body.style.userSelect = "";
			MapKit.relayout();
			try {
				const width = document.getElementById("tripInfoPanel").getBoundingClientRect().width;
				localStorage.setItem("petcheck.tripPanelWidth", String(Math.round(width)));
			} catch (e) {
				// 저장 실패는 무시 — 다음 방문 때 기본폭으로 보이는 것뿐이다
			}
		});
	}

	(async () => {
		await MapKit.init("map", KAKAO_JS_KEY);
		await reload();
	})();
})();
