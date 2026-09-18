(function () {
	const listEl = document.getElementById("storeList");
	const tabs = document.querySelectorAll("#statusTabs [data-status]");
	let status = "PENDING";

	async function load() {
		try {
			const page = await Api.get(`/api/admin/stores?status=${status}&size=50`);
			render(page.content);
		} catch (e) {
			listEl.innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`;
		}
	}

	function render(stores) {
		if (stores.length === 0) {
			listEl.innerHTML = '<div class="empty-state">해당 상태의 매장이 없습니다.</div>';
			return;
		}
		listEl.innerHTML = stores.map((s) => `
			<div class="store-card">
				<div class="store-card-head">
					<h3>${escapeHtml(s.businessName)}</h3>
					<span class="status-badge ${s.verificationStatus}">${s.verificationStatus}</span>
				</div>
				<div class="store-card-meta">
					${escapeHtml(s.placeName)} · 사업자번호 ${escapeHtml(s.businessNumber)}<br>
					법정기준 체크리스트 ${s.satisfiedCount}/8 충족
					${s.rejectReason ? `<br>반려 사유: ${escapeHtml(s.rejectReason)}` : ""}
				</div>
				${s.proofImageUrl ? `<img class="proof-thumb" src="${s.proofImageUrl}" alt="증빙 사진">` : '<div class="field-hint">증빙 사진 없음</div>'}
				${status === "PENDING" ? `
					<div class="store-card-actions">
						<button type="button" data-approve="${s.id}">승인</button>
						<button type="button" data-reject="${s.id}">반려</button>
					</div>
				` : ""}
			</div>
		`).join("");

		listEl.querySelectorAll("[data-approve]").forEach((btn) => {
			btn.addEventListener("click", async () => {
				if (!confirm("이 매장을 승인하시겠습니까?")) return;
				try {
					await Api.post(`/api/admin/stores/${btn.dataset.approve}/approve`);
					await load();
				} catch (e) {
					alert(e.message);
				}
			});
		});
		listEl.querySelectorAll("[data-reject]").forEach((btn) => {
			btn.addEventListener("click", async () => {
				const reason = prompt("반려 사유를 입력해주세요.");
				if (!reason) return;
				try {
					await Api.post(`/api/admin/stores/${btn.dataset.reject}/reject`, { reason });
					await load();
				} catch (e) {
					alert(e.message);
				}
			});
		});
	}

	tabs.forEach((tab) => {
		tab.addEventListener("click", () => {
			status = tab.dataset.status;
			tabs.forEach((t) => t.classList.toggle("active", t === tab));
			load();
		});
	});

	load();
})();
