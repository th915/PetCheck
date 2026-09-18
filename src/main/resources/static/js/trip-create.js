(function () {
	const draft = { title: "", startDate: null, endDate: null, places: [] };
	let selectedDate = null;
	let nextKey = 1;

	const dateTabsEl = document.getElementById("dateTabs");
	const tripListEl = document.getElementById("tripPlaceList");
	const titleInput = document.getElementById("tripTitle");
	const startInput = document.getElementById("startDate");
	const endInput = document.getElementById("endDate");

	function currentDates() {
		return draft.startDate && draft.endDate ? TripCommon.dateRange(draft.startDate, draft.endDate) : [];
	}

	function renumberDate(date) {
		const items = draft.places.filter((p) => p.visitDate === date).sort((a, b) => a.sortOrder - b.sortOrder);
		items.forEach((p, i) => (p.sortOrder = i));
	}

	function selectDate(d) {
		selectedDate = d;
		TripCommon.renderDateTabs(dateTabsEl, currentDates(), selectedDate, selectDate);
		renderMap();
	}

	function renderMap() {
		MapKit.clearAll();
		const daySorted = draft.places.filter((p) => p.visitDate === selectedDate).sort((a, b) => a.sortOrder - b.sortOrder);
		const orderIndex = new Map(daySorted.map((p, i) => [p.key, i + 1]));
		draft.places.forEach((p) => {
			const label = orderIndex.has(p.key) ? String(orderIndex.get(p.key)) : null;
			MapKit.addMarker(p.latitude, p.longitude, label, () => PlacePopup.open(p.placeId));
		});
		MapKit.drawPolyline(daySorted.map((p) => ({ lat: p.latitude, lng: p.longitude })), "#d64545");
		if (draft.places.length) MapKit.fitBounds(draft.places.map((p) => ({ lat: p.latitude, lng: p.longitude })));
	}

	function rerenderAll() {
		const dates = currentDates();
		if (dates.length && !dates.includes(selectedDate)) selectedDate = dates[0];
		if (!dates.length) selectedDate = null;
		TripCommon.renderDateTabs(dateTabsEl, dates, selectedDate, selectDate);
		TripCommon.renderPlaceList(tripListEl, draft.places, dates, { onDateChange, onTimeChange, onRemove, onReorder });
		renderMap();
	}

	function addPlace(place) {
		if (!draft.startDate || !draft.endDate) {
			UIDialog.alert("먼저 여행 날짜를 입력해주세요.");
			return;
		}
		if (draft.places.some((p) => p.placeId === place.id)) {
			UIDialog.alert("이미 등록된 장소입니다.");
			return;
		}
		const targetDate = draft.startDate;
		const count = draft.places.filter((p) => p.visitDate === targetDate).length;
		draft.places.push({
			key: String(nextKey++),
			placeId: place.id,
			name: place.name,
			address: place.address,
			latitude: place.latitude,
			longitude: place.longitude,
			visitDate: targetDate,
			sortOrder: count,
			reservationTime: null,
		});
		rerenderAll();
	}

	function onDateChange(key, newDate) {
		const p = draft.places.find((x) => x.key === key);
		if (!p) return;
		const oldDate = p.visitDate;
		p.visitDate = newDate;
		p.sortOrder = draft.places.filter((x) => x.visitDate === newDate && x !== p).length;
		renumberDate(oldDate);
		rerenderAll();
	}

	function onTimeChange(key, value) {
		const p = draft.places.find((x) => x.key === key);
		if (p) p.reservationTime = value;
	}

	function onRemove(key) {
		const idx = draft.places.findIndex((p) => p.key === key);
		if (idx === -1) return;
		const date = draft.places[idx].visitDate;
		draft.places.splice(idx, 1);
		renumberDate(date);
		rerenderAll();
	}

	function onReorder(date, orderedKeys) {
		orderedKeys.forEach((k, i) => {
			const p = draft.places.find((x) => x.key === k);
			if (p) p.sortOrder = i;
		});
		renderMap();
	}

	function onDatesChanged() {
		draft.startDate = startInput.value || null;
		draft.endDate = endInput.value || null;
		if (draft.startDate && draft.endDate && draft.endDate < draft.startDate) {
			UIDialog.alert("종료일은 시작일보다 빠를 수 없습니다.");
			endInput.value = draft.startDate;
			draft.endDate = draft.startDate;
		}
		const dates = currentDates();
		if (dates.length) {
			draft.places.forEach((p) => {
				if (!dates.includes(p.visitDate)) {
					p.visitDate = p.visitDate < draft.startDate ? draft.startDate : draft.endDate;
				}
			});
			dates.forEach((d) => renumberDate(d));
		}
		rerenderAll();
	}

	async function submit() {
		draft.title = titleInput.value.trim();
		if (!draft.title) {
			UIDialog.alert("여행 이름을 입력해주세요.");
			return;
		}
		if (!draft.startDate || !draft.endDate) {
			UIDialog.alert("여행 날짜를 입력해주세요.");
			return;
		}
		const payload = {
			title: draft.title,
			startDate: draft.startDate,
			endDate: draft.endDate,
			places: draft.places.map((p) => ({
				placeId: p.placeId,
				visitDate: p.visitDate,
				sortOrder: p.sortOrder,
				reservationTime: p.reservationTime || null,
			})),
		};
		try {
			const result = await Api.post("/api/trips", payload);
			location.href = `/trips/${result.id}`;
		} catch (e) {
			UIDialog.alert(e.message);
		}
	}

	startInput.addEventListener("change", onDatesChanged);
	endInput.addEventListener("change", onDatesChanged);
	document.getElementById("registerBtn").addEventListener("click", submit);

	MapKit.init("map", KAKAO_JS_KEY).then(() => {
		createPlaceSearchPanel({
			keywordInputId: "keywordInput",
			listId: "placeList",
			sortBarId: "sortBar",
			certifiedBtnId: "certifiedBtn",
			favoritesBtnId: "favoritesBtn",
			showAddButton: true,
			onAddToTrip: addPlace,
		});
	});

	rerenderAll();
})();
