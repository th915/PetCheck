const CATEGORY_KEYWORD_MAP = {
	"식당": "RESTAURANT", "레스토랑": "RESTAURANT",
	"카페": "CAFE",
	"숙박": "ACCOMMODATION", "호텔": "ACCOMMODATION", "펜션": "ACCOMMODATION",
	"문화시설": "CULTURE", "문화": "CULTURE",
	"관광지": "TOURIST_SPOT",
	"펫시터": "PET_SITTER",
	"애견호텔": "PET_HOTEL",
	"애견유치원": "PET_KINDERGARTEN", "유치원": "PET_KINDERGARTEN",
	"동물병원": "VET", "병원": "VET",
	"동물약국": "PET_PHARMACY",
	"펫샵": "PET_SUPPLY", "용품": "PET_SUPPLY",
	"미용": "PET_GROOMING", "애견미용": "PET_GROOMING",
};

function createPlaceSearchPanel(opts) {
	const state = {
		keyword: "",
		sort: "TRUST_SCORE",
		certifiedOnly: false,
		favoritesOnly: false,
		userLat: null,
		userLng: null,
		results: [],
		favoriteIds: new Set(),
	};

	const keywordInput = document.getElementById(opts.keywordInputId);
	const listEl = document.getElementById(opts.listId);
	const sortButtons = document.querySelectorAll(`#${opts.sortBarId} [data-sort]`);
	const certifiedBtn = document.getElementById(opts.certifiedBtnId);
	const favoritesBtn = opts.favoritesBtnId ? document.getElementById(opts.favoritesBtnId) : null;

	function buildQuery() {
		const params = new URLSearchParams();
		if (state.keyword) {
			const mapped = CATEGORY_KEYWORD_MAP[state.keyword.replace(/\s/g, "")];
			if (mapped) {
				params.set("categories", mapped);
			} else {
				params.set("keyword", state.keyword);
			}
		}
		params.set("sort", state.sort);
		if (state.certifiedOnly) params.set("legalCertifiedOnly", "true");
		if (state.userLat != null) params.set("latitude", state.userLat);
		if (state.userLng != null) params.set("longitude", state.userLng);
		params.set("limit", "300");
		return params.toString();
	}

	async function loadFavoriteIds() {
		if (!PlacePopup.isLoggedIn) {
			state.favoriteIds = new Set();
			return;
		}
		try {
			const ids = await Api.get("/api/favorites");
			state.favoriteIds = new Set(ids);
		} catch (e) {
			state.favoriteIds = new Set();
		}
	}

	async function fetchResults() {
		try {
			await loadFavoriteIds();
			let results = await Api.get(`/api/places/map?${buildQuery()}`);
			results = results.map((p) => ({
				...p,
				favorited: state.favoriteIds.has(p.id),
				distanceKm: distanceKm(state.userLat, state.userLng, p.latitude, p.longitude),
			}));
			if (state.favoritesOnly) {
				results = results.filter((p) => p.favorited);
			}
			state.results = results;
			render();
			if (opts.onResults) opts.onResults(results);
		} catch (e) {
			listEl.innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`;
		}
	}

	function render() {
		if (state.results.length === 0) {
			listEl.innerHTML = '<div class="empty-state">검색 결과가 없습니다.</div>';
			return;
		}
		listEl.innerHTML = state.results.map((p) => `
			<div class="place-row" data-place-id="${p.id}">
				<button class="icon-btn ${p.favorited ? "active" : ""}" data-fav-btn="${p.id}">${p.favorited ? "♥" : "♡"}</button>
				<div class="place-main">
					<div class="place-name" data-open-popup="${p.id}">${escapeHtml(p.name)}</div>
					<div class="place-meta">
						${escapeHtml(categoryLabel(p.category))}
						${p.distanceKm != null ? ` · ${p.distanceKm.toFixed(1)}km` : ""}
						· 리뷰 ${p.reviewCount || 0}
						${p.legalCertified === true ? ' · <span class="badge-certified">★ 인증</span>' : ""}
					</div>
				</div>
				${opts.showAddButton ? `<button class="add-to-trip-btn" data-add-btn="${p.id}">${opts.addButtonLabel || "일정 등록"}</button>` : ""}
			</div>
		`).join("");

		listEl.querySelectorAll("[data-open-popup]").forEach((el) => {
			el.addEventListener("click", () => PlacePopup.open(el.dataset.openPopup));
		});
		listEl.querySelectorAll("[data-fav-btn]").forEach((el) => {
			el.addEventListener("click", async () => {
				const placeId = el.dataset.favBtn;
				const result = await PlacePopup.toggleFavoriteRaw(placeId);
				if (result) {
					el.textContent = result.favorited ? "♥" : "♡";
					el.classList.toggle("active", result.favorited);
				}
			});
		});
		if (opts.showAddButton) {
			listEl.querySelectorAll("[data-add-btn]").forEach((el) => {
				el.addEventListener("click", () => {
					const place = state.results.find((p) => String(p.id) === el.dataset.addBtn);
					if (place && opts.onAddToTrip) opts.onAddToTrip(place);
				});
			});
		}
	}

	keywordInput.addEventListener("keydown", (e) => {
		if (e.key === "Enter") {
			state.keyword = keywordInput.value.trim();
			fetchResults();
		}
	});

	sortButtons.forEach((btn) => {
		btn.addEventListener("click", () => {
			state.sort = btn.dataset.sort;
			sortButtons.forEach((b) => b.classList.toggle("active", b === btn));
			if (state.sort === "DISTANCE" && state.userLat == null) {
				requestGeolocation().then(fetchResults);
			} else {
				fetchResults();
			}
		});
	});

	if (certifiedBtn) {
		certifiedBtn.addEventListener("click", () => {
			state.certifiedOnly = !state.certifiedOnly;
			certifiedBtn.classList.toggle("active", state.certifiedOnly);
			fetchResults();
		});
	}

	if (favoritesBtn) {
		favoritesBtn.addEventListener("click", () => {
			state.favoritesOnly = !state.favoritesOnly;
			favoritesBtn.classList.toggle("active", state.favoritesOnly);
			fetchResults();
		});
	}

	function requestGeolocation() {
		return new Promise((resolve) => {
			if (!navigator.geolocation) {
				resolve();
				return;
			}
			navigator.geolocation.getCurrentPosition(
				(pos) => {
					state.userLat = pos.coords.latitude;
					state.userLng = pos.coords.longitude;
					resolve();
				},
				() => resolve(),
				{ timeout: 4000 }
			);
		});
	}

	function updateFavoriteBadge(placeId, favorited) {
		const row = listEl.querySelector(`[data-fav-btn="${placeId}"]`);
		if (row) {
			row.textContent = favorited ? "♥" : "♡";
			row.classList.toggle("active", favorited);
		}
	}

	fetchResults();

	return { refresh: fetchResults, updateFavoriteBadge, state };
}
