(function () {
	const listEl = document.getElementById("storeList");

	const STATUS_LABEL = { PENDING: "승인 대기", APPROVED: "승인됨", REJECTED: "반려됨" };

	async function load() {
		try {
			const stores = await Api.get("/api/store/me");
			render(stores);
		} catch (e) {
			listEl.innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`;
		}
	}

	function render(stores) {
		if (stores.length === 0) {
			listEl.innerHTML = '<div class="empty-state">등록된 매장이 없습니다.<br>위 "매장 등록" 버튼으로 첫 매장을 등록해보세요.</div>';
			return;
		}
		listEl.innerHTML = stores.map((s) => `
			<div class="store-card">
				<div class="store-card-head">
					<h3>${escapeHtml(s.businessName)}</h3>
					<span class="status-badge ${s.verificationStatus}">${STATUS_LABEL[s.verificationStatus] || s.verificationStatus}</span>
				</div>
				<div class="store-card-meta">
					${escapeHtml(s.placeName)} · 사업자번호 ${escapeHtml(s.businessNumber)}<br>
					법정기준 체크리스트 ${s.satisfiedCount}/8 충족
					${s.legalCertified ? ' · <span class="badge-certified">★ 법정기준 인증</span>' : ""}
					${s.verificationStatus === "REJECTED" && s.rejectReason ? `<br>반려 사유: ${escapeHtml(s.rejectReason)}` : ""}
				</div>
				<div class="store-card-actions">
					<a href="/store/${s.id}/checklist">체크리스트 수정</a>
					<button type="button" data-delete="${s.id}">매장 삭제</button>
				</div>
			</div>
		`).join("");

		listEl.querySelectorAll("[data-delete]").forEach((btn) => {
			btn.addEventListener("click", async () => {
				if (!confirm("이 매장을 삭제하시겠습니까? 인증 상태가 초기화됩니다.")) return;
				try {
					await Api.del(`/api/store/${btn.dataset.delete}`);
					await load();
				} catch (e) {
					alert(e.message);
				}
			});
		});
	}

	load();
})();
