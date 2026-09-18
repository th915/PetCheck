const PlacePopup = {
	onFavoriteToggled: null,
	isLoggedIn: false,
	isCustomer: false,
	currentPlaceId: null,

	async open(placeId) {
		const el = document.getElementById("placePopup");
		if (!el) return;
		this.currentPlaceId = String(placeId);
		el.classList.add("open");
		el.innerHTML = '<div class="empty-state">불러오는 중...</div>';
		try {
			const [place, favoriteIds, reviews] = await Promise.all([
				Api.get(`/api/places/${placeId}`),
				this.isLoggedIn ? Api.get("/api/favorites").catch(() => []) : Promise.resolve([]),
				Api.get(`/api/reviews?placeId=${placeId}&size=20`).catch(() => ({ content: [] })),
			]);
			place.favorited = favoriteIds.includes(place.id);
			el.innerHTML = this.render(place, reviews.content || []);
			if (typeof ReviewWrite !== "undefined") {
				ReviewWrite.bind(place.id);
			}
		} catch (e) {
			el.innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`;
		}
	},

	close() {
		const el = document.getElementById("placePopup");
		if (el) {
			el.classList.remove("open");
			el.innerHTML = "";
		}
		this.currentPlaceId = null;
	},

	render(place, reviews) {
		const conditions = [];
		const policy = place.petPolicy || {};
		if (policy.allowSmall) conditions.push("소형견 가능");
		if (policy.allowMedium) conditions.push("중형견 가능");
		if (policy.allowLarge) conditions.push("대형견 가능");
		if (policy.indoorAllowed) conditions.push("실내 동반");
		if (policy.outdoorAllowed) conditions.push("야외 동반");
		if (policy.leashRequired) conditions.push("목줄 필수");
		if (policy.carrierRequired) conditions.push("이동장 필수");
		if (policy.vaccinationRequired) conditions.push("예방접종 증빙");
		if (policy.extraFee) conditions.push(`추가요금 ${policy.extraFee.toLocaleString()}원`);
		if (policy.weightLimitKg) conditions.push(`체중제한 ${policy.weightLimitKg}kg`);

		const certified = place.store && place.store.legalCertified;

		const reviewsHtml = reviews.length
			? reviews.map((r) => this.renderReview(r)).join("")
			: '<div class="popup-section" style="color:#8a8078;">아직 등록된 리뷰가 없습니다.</div>';

		return `
			<button class="popup-close" onclick="PlacePopup.close()">✕</button>
			<h3>
				${escapeHtml(place.name)}
				${certified ? '<span class="badge-certified" title="법정기준 인증 매장">★</span>' : ""}
				<button class="icon-btn ${place.favorited ? "active" : ""}" id="popupFavoriteBtn"
					onclick="PlacePopup.toggleFavorite(${place.id})">${place.favorited ? "♥" : "♡"}</button>
			</h3>
			<div class="popup-address">${escapeHtml(place.address)}</div>
			<div class="popup-section">
				<span class="tag">${escapeHtml(categoryLabel(place.category))}</span>
				<span class="tag">리뷰 ${place.reviewCount}개</span>
				<span class="tag">신뢰도 ${Math.round((place.trustScore || 0) * 100)}점 (${escapeHtml(place.trustGrade || "")})</span>
			</div>
			${conditions.length ? `<div class="popup-section">${conditions.map((c) => `<span class="tag">${escapeHtml(c)}</span>`).join("")}</div>` : ""}
			${policy.restrictionNote ? `<div class="popup-section">${escapeHtml(policy.restrictionNote)}</div>` : ""}
			${place.store ? `<div class="popup-section"><strong>${escapeHtml(place.store.businessName)}</strong> ${certified ? "· 법정기준 인증 매장" : "· 매장 등록됨 (인증 대기)"}</div>` : ""}

			<div id="reviewWriteSlot"></div>

			<div class="popup-section" style="font-weight:700;">리뷰 (${reviews.length})</div>
			<div id="popupReviewList">${reviewsHtml}</div>
		`;
	},

	renderReview(r) {
		const statusTag = {
			GENUINE: "", SUSPICIOUS: '<span class="tag review-flag">AI 의심</span>',
			PENDING: '<span class="tag">판정 대기</span>', FAKE: '<span class="tag review-flag">가짜 의심</span>',
		}[r.authenticityStatus] || "";
		const levelLabel = { RECEIPT: "영수증 인증", PHOTO_EXIF: "사진 인증", LOCATION: "위치 인증", SCHEDULE: "일정 인증", NONE: "미인증" }[r.verificationLevel] || r.verificationLevel;
		return `
			<div class="review-item">
				<span class="review-nickname">${escapeHtml(r.writerNickname)}</span>
				<span class="review-date">${escapeHtml(r.visitedAt || "")}</span>
				<span class="tag">${r.matched ? "정보 일치" : "정보 불일치"}</span>
				<span class="tag">${escapeHtml(levelLabel)}</span>
				${statusTag}
				<div>${escapeHtml(r.content || "")}</div>
			</div>
		`;
	},

	async toggleFavorite(placeId) {
		const result = await this.toggleFavoriteRaw(placeId);
		if (!result) return;
		const btn = document.getElementById("popupFavoriteBtn");
		if (btn) {
			btn.textContent = result.favorited ? "♥" : "♡";
			btn.classList.toggle("active", result.favorited);
		}
	},

	async toggleFavoriteRaw(placeId) {
		if (!this.isLoggedIn) {
			if (confirm("로그인이 필요합니다. 로그인 페이지로 이동할까요?")) {
				location.href = "/login";
			}
			return null;
		}
		try {
			const result = await Api.post(`/api/favorites/${placeId}`);
			if (this.onFavoriteToggled) this.onFavoriteToggled(placeId, result.favorited);
			return result;
		} catch (e) {
			alert(e.message);
			return null;
		}
	},
};
