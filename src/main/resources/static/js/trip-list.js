(function () {
	const monthLabelEl = document.getElementById("monthLabel");
	const gridEl = document.getElementById("calendarGrid");
	const flatListEl = document.getElementById("tripFlatList");

	const today = new Date();
	let viewYear = today.getFullYear();
	let viewMonth = today.getMonth(); // 0-indexed
	let trips = [];

	const DOW = ["일", "월", "화", "수", "목", "금", "토"];

	function pad(n) {
		return String(n).padStart(2, "0");
	}

	function dateStr(y, m, d) {
		return `${y}-${pad(m + 1)}-${pad(d)}`;
	}

	function tripsOnDate(dstr) {
		return trips.filter((t) => t.startDate <= dstr && dstr <= t.endDate);
	}

	function renderCalendar() {
		monthLabelEl.textContent = `${viewYear}년 ${viewMonth + 1}월`;
		const firstDow = new Date(viewYear, viewMonth, 1).getDay();
		const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();

		let html = DOW.map((d) => `<div class="calendar-dow">${d}</div>`).join("");
		for (let i = 0; i < firstDow; i++) {
			html += '<div class="calendar-cell empty"></div>';
		}
		const todayStr = dateStr(today.getFullYear(), today.getMonth(), today.getDate());
		for (let d = 1; d <= daysInMonth; d++) {
			const dstr = dateStr(viewYear, viewMonth, d);
			const dayTrips = tripsOnDate(dstr);
			const isToday = dstr === todayStr;
			html += `
				<div class="calendar-cell${isToday ? " today" : ""}">
					<div class="cell-date">${d}${isToday ? '<span class="today-badge">오늘</span>' : ""}</div>
					${dayTrips.map((t) => `<div class="calendar-trip-chip" data-trip-id="${t.id}" title="${escapeHtml(t.title)}">${escapeHtml(t.title)}</div>`).join("")}
				</div>
			`;
		}
		gridEl.innerHTML = html;
		gridEl.querySelectorAll("[data-trip-id]").forEach((el) => {
			el.addEventListener("click", () => (location.href = `/trips/${el.dataset.tripId}`));
		});
	}

	function renderFlatList() {
		if (trips.length === 0) {
			flatListEl.innerHTML = '<div class="empty-state">등록된 여행 일정이 없습니다.</div>';
			return;
		}
		flatListEl.innerHTML = trips.map((t) => `
			<div class="trip-summary-row">
				<div>
					<div class="ts-title" data-open="${t.id}">${escapeHtml(t.title)}</div>
					<div class="ts-dates">${t.startDate} ~ ${t.endDate}</div>
				</div>
				<button class="trip-delete-btn" data-delete="${t.id}">삭제</button>
			</div>
		`).join("");
		flatListEl.querySelectorAll("[data-open]").forEach((el) => {
			el.addEventListener("click", () => (location.href = `/trips/${el.dataset.open}`));
		});
		flatListEl.querySelectorAll("[data-delete]").forEach((el) => {
			el.addEventListener("click", async () => {
				if (!(await UIDialog.confirm("이 여행 일정을 삭제하시겠습니까?"))) return;
				try {
					await Api.del(`/api/trips/${el.dataset.delete}`);
					await load();
				} catch (e) {
					UIDialog.alert(e.message);
				}
			});
		});
	}

	async function load() {
		try {
			trips = await Api.get("/api/trips");
			renderCalendar();
			renderFlatList();
		} catch (e) {
			flatListEl.innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`;
		}
	}

	document.getElementById("prevMonth").addEventListener("click", () => {
		viewMonth--;
		if (viewMonth < 0) {
			viewMonth = 11;
			viewYear--;
		}
		renderCalendar();
	});
	document.getElementById("nextMonth").addEventListener("click", () => {
		viewMonth++;
		if (viewMonth > 11) {
			viewMonth = 0;
			viewYear++;
		}
		renderCalendar();
	});

	load();
})();
