const TripCommon = {
	toDateStr(d) {
		const y = d.getFullYear();
		const m = String(d.getMonth() + 1).padStart(2, "0");
		const dd = String(d.getDate()).padStart(2, "0");
		return `${y}-${m}-${dd}`;
	},

	parseDateStr(dateStr) {
		const [y, m, d] = dateStr.split("-").map(Number);
		return new Date(y, m - 1, d);
	},

	dateRange(startDate, endDate) {
		const dates = [];
		let cur = this.parseDateStr(startDate);
		const end = this.parseDateStr(endDate);
		while (cur <= end) {
			dates.push(this.toDateStr(cur));
			cur.setDate(cur.getDate() + 1);
		}
		return dates;
	},

	formatDateLabel(dateStr) {
		const d = this.parseDateStr(dateStr);
		return `${d.getMonth() + 1}.${d.getDate()}`;
	},

	renderDateTabs(containerEl, dates, selectedDate, onSelect) {
		if (!dates || dates.length === 0) {
			containerEl.innerHTML = "";
			return;
		}
		containerEl.innerHTML = dates.map((d, i) => `
			<button data-date="${d}" class="${d === selectedDate ? "active" : ""}">${i + 1}일차 ${this.formatDateLabel(d)}</button>
		`).join("");
		containerEl.querySelectorAll("button").forEach((btn) => {
			btn.addEventListener("click", () => onSelect(btn.dataset.date));
		});
	},

	groupByDate(places) {
		const map = new Map();
		places.forEach((p) => {
			if (!map.has(p.visitDate)) map.set(p.visitDate, []);
			map.get(p.visitDate).push(p);
		});
		map.forEach((list) => list.sort((a, b) => a.sortOrder - b.sortOrder));
		return map;
	},

	renderPlaceList(containerEl, places, dates, handlers, options) {
		const opts = options || {};
		const grouped = this.groupByDate(places);
		if (places.length === 0) {
			containerEl.innerHTML = '<div class="empty-state">등록된 장소가 없습니다.<br>왼쪽에서 장소를 검색해 추가해보세요.</div>';
			return;
		}
		let html = "";
		const dayList = dates && dates.length ? dates : Array.from(grouped.keys()).sort();
		const selectDates = opts.selectDates && opts.selectDates.length ? opts.selectDates : dayList;
		dayList.forEach((date) => {
			const idx = selectDates.indexOf(date) >= 0 ? selectDates.indexOf(date) : dayList.indexOf(date);
			const items = grouped.get(date) || [];
			if (items.length === 0 && dates) {
				html += `<div class="day-divider">${idx + 1}일차 (${this.formatDateLabel(date)}) · 등록된 장소 없음</div>`;
				return;
			}
			if (items.length === 0) return;
			html += `<div class="day-divider">${idx + 1}일차 (${this.formatDateLabel(date)})</div>`;
			items.forEach((p) => {
				html += `
					<div class="trip-place-item" draggable="true" data-key="${p.key}" data-date="${p.visitDate}">
						<span class="drag-handle">⠿</span>
						<div class="tp-main">
							<div class="tp-name" data-open-popup="${p.placeId}">
								${escapeHtml(p.name)}
								${opts.showFavorite ? `<button class="icon-btn ${p.favorited ? "active" : ""}" data-fav="${p.placeId}" data-fav-key="${p.key}" onclick="event.stopPropagation()">${p.favorited ? "♥" : "♡"}</button>` : ""}
							</div>
							<div class="tp-meta">
								<select class="tp-date-select" data-date-select="${p.key}">
									${selectDates.map((d, i) => `<option value="${d}" ${d === p.visitDate ? "selected" : ""}>${i + 1}일차 (${this.formatDateLabel(d)})</option>`).join("")}
								</select>
								<input type="time" class="tp-time-input" data-time-input="${p.key}" value="${p.reservationTime ? p.reservationTime.substring(0, 5) : ""}">
							</div>
						</div>
						<button class="tp-remove" data-remove="${p.key}">✕</button>
					</div>
				`;
			});
		});
		containerEl.innerHTML = html;

		containerEl.querySelectorAll("[data-open-popup]").forEach((el) => {
			el.addEventListener("click", () => PlacePopup.open(el.dataset.openPopup));
		});
		containerEl.querySelectorAll("[data-date-select]").forEach((el) => {
			el.addEventListener("change", () => handlers.onDateChange(el.dataset.dateSelect, el.value));
		});
		containerEl.querySelectorAll("[data-time-input]").forEach((el) => {
			el.addEventListener("change", () => handlers.onTimeChange(el.dataset.timeInput, el.value || null));
		});
		containerEl.querySelectorAll("[data-remove]").forEach((el) => {
			el.addEventListener("click", () => handlers.onRemove(el.dataset.remove));
		});
		containerEl.querySelectorAll("[data-fav]").forEach((el) => {
			el.addEventListener("click", async () => {
				const result = await PlacePopup.toggleFavoriteRaw(el.dataset.fav);
				if (result) {
					el.textContent = result.favorited ? "♥" : "♡";
					el.classList.toggle("active", result.favorited);
				}
			});
		});

		let dragKey = null;
		containerEl.querySelectorAll(".trip-place-item").forEach((row) => {
			row.addEventListener("dragstart", () => {
				dragKey = row.dataset.key;
				row.classList.add("dragging");
			});
			row.addEventListener("dragend", () => row.classList.remove("dragging"));
			row.addEventListener("dragover", (e) => {
				const draggedRow = containerEl.querySelector(`[data-key="${dragKey}"]`);
				if (!draggedRow || draggedRow.dataset.date !== row.dataset.date) return;
				e.preventDefault();
				const rect = row.getBoundingClientRect();
				const before = (e.clientY - rect.top) < rect.height / 2;
				row.parentNode.insertBefore(draggedRow, before ? row : row.nextSibling);
			});
			row.addEventListener("drop", (e) => {
				e.preventDefault();
				const date = row.dataset.date;
				const orderedKeys = Array.from(containerEl.querySelectorAll(`.trip-place-item[data-date="${date}"]`))
					.map((r) => r.dataset.key);
				handlers.onReorder(date, orderedKeys);
			});
		});
	},

	routePointsForDate(places, date) {
		return places
			.filter((p) => p.visitDate === date)
			.sort((a, b) => a.sortOrder - b.sortOrder)
			.map((p) => ({ lat: p.latitude, lng: p.longitude, name: p.name }));
	},
};
