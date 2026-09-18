const CATEGORY_KEYWORD_MAP = {
	"식당": "RESTAURANT", "레스토랑": "RESTAURANT", "맛집": "RESTAURANT",
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

/** "대형견과 동반 가능한 식당" 같은 자연어 검색어를 구조화된 필터로 풀어낸다 */
const PET_SIZE_KEYWORD_MAP = {
	"소형견": "SMALL", "소형": "SMALL",
	"중형견": "MEDIUM", "중형": "MEDIUM",
	"대형견": "LARGE", "대형": "LARGE",
};

const NO_CARRIER_KEYWORDS = [
	"케이지없이", "케이지필요없이", "케이지불필요", "케이지없는",
	"캐리어없이", "캐리어필요없이", "캐리어불필요", "캐리어없는",
];

const INDOOR_KEYWORDS = ["실내동반", "실내가능", "실내출입"];

/**
 * 공백을 지운 문자열에서 부분 일치를 본다. 조사("과", "와", "을")가 붙어도
 * "대형견과" 안에 "대형견"이 그대로 들어있으므로 이 방식으로 충분히 잡힌다.
 * 구조화된 조건이 하나라도 잡히면 원문을 keyword 로는 보내지 않는다 —
 * "대형견과 동반 가능한 식당"을 그대로 이름·주소 검색에 넣으면 결과가 0건이 된다.
 */
function parseSearchInput(rawInput) {
	const norm = rawInput.replace(/\s/g, "");
	const parsed = {};
	let matchedAny = false;

	for (const [word, size] of Object.entries(PET_SIZE_KEYWORD_MAP)) {
		if (norm.includes(word)) {
			parsed.petSize = size;
			matchedAny = true;
			break;
		}
	}
	if (NO_CARRIER_KEYWORDS.some((w) => norm.includes(w))) {
		parsed.noCarrierOnly = true;
		matchedAny = true;
	}
	if (INDOOR_KEYWORDS.some((w) => norm.includes(w))) {
		parsed.indoorRequired = true;
		matchedAny = true;
	}
	for (const [word, category] of Object.entries(CATEGORY_KEYWORD_MAP)) {
		if (norm.includes(word)) {
			parsed.categories = category;
			matchedAny = true;
			break;
		}
	}

	if (!matchedAny) {
		parsed.keyword = rawInput;
	}
	return parsed;
}

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
		/** 우리 DB에 없어 카카오맵에서만 찾은 장소. 즐겨찾기·일정·리뷰를 시도하는 순간 실체화된다 */
		externalResults: [],
		externalKeywordFetched: null,
	};

	const keywordInput = document.getElementById(opts.keywordInputId);
	const listEl = document.getElementById(opts.listId);
	const sortButtons = document.querySelectorAll(`#${opts.sortBarId} [data-sort]`);
	const certifiedBtn = document.getElementById(opts.certifiedBtnId);
	const favoritesBtn = opts.favoritesBtnId ? document.getElementById(opts.favoritesBtnId) : null;

	function buildQuery() {
		const params = new URLSearchParams();
		if (state.keyword) {
			const parsed = parseSearchInput(state.keyword);
			if (parsed.categories) params.set("categories", parsed.categories);
			if (parsed.petSize) params.set("petSize", parsed.petSize);
			if (parsed.noCarrierOnly) params.set("noCarrierOnly", "true");
			if (parsed.indoorRequired) params.set("indoorRequired", "true");
			if (parsed.keyword) params.set("keyword", parsed.keyword);
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
			await fetchExternalResults();
			render();
			if (opts.onResults) opts.onResults(state.results, state.externalResults);
		} catch (e) {
			listEl.innerHTML = `<div class="empty-state">${escapeHtml(e.message)}</div>`;
		}
	}

	/**
	 * 공공데이터 4종에 없는 장소도 카카오맵에서 찾아 보여준다.
	 * 정렬·인증·즐겨찾기 필터 클릭마다 다시 부를 필요는 없으므로 같은 검색어면 건너뛴다.
	 */
	async function fetchExternalResults() {
		if (!state.keyword) {
			state.externalResults = [];
			state.externalKeywordFetched = null;
			return;
		}
		if (state.externalKeywordFetched === state.keyword) {
			return;
		}
		try {
			const params = new URLSearchParams({ keyword: state.keyword });
			if (state.userLat != null) params.set("latitude", state.userLat);
			if (state.userLng != null) params.set("longitude", state.userLng);
			const results = await Api.get(`/api/places/external-search?${params.toString()}`);
			state.externalResults = results.map((p) => ({
				...p,
				favorited: false,
				distanceKm: distanceKm(state.userLat, state.userLng, p.latitude, p.longitude),
			}));
			state.externalKeywordFetched = state.keyword;
		} catch (e) {
			state.externalResults = [];
		}
	}

	/** DB 결과는 숫자 id, 카카오 전용 결과는 "kakao:<kakaoId>" 로 행을 구분한다 */
	function rowKey(p, isExternal) {
		return isExternal ? `kakao:${p.kakaoId}` : String(p.id);
	}

	function findByKey(key) {
		if (key.startsWith("kakao:")) {
			const kakaoId = key.slice("kakao:".length);
			return state.externalResults.find((p) => p.kakaoId === kakaoId);
		}
		return state.results.find((p) => String(p.id) === key);
	}

	function renderRow(p, isExternal) {
		const key = rowKey(p, isExternal);
		return `
			<div class="place-row" data-key="${key}">
				<button class="icon-btn ${p.favorited ? "active" : ""}" data-fav-btn="${key}">${p.favorited ? "♥" : "♡"}</button>
				<div class="place-main">
					<div class="place-name" data-open-popup="${key}">
						${escapeHtml(p.name)}
						${isExternal ? '<span class="badge-kakao" title="공공데이터에는 없는, 카카오맵 검색 결과입니다">카카오</span>' : ""}
					</div>
					<div class="place-meta">
						${escapeHtml(categoryLabel(p.category))}
						${p.distanceKm != null ? ` · ${p.distanceKm.toFixed(1)}km` : ""}
						${isExternal ? "" : ` · 리뷰 ${p.reviewCount || 0}`}
						${p.legalCertified === true ? ' · <span class="badge-certified">★ 인증</span>' : ""}
					</div>
				</div>
				${opts.showAddButton ? `<button class="add-to-trip-btn" data-add-btn="${key}">${opts.addButtonLabel || "일정 등록"}</button>` : ""}
			</div>
		`;
	}

	function render() {
		const dbHtml = state.results.map((p) => renderRow(p, false)).join("");
		const externalHtml = state.externalResults.map((p) => renderRow(p, true)).join("");

		if (!dbHtml && !externalHtml) {
			listEl.innerHTML = '<div class="empty-state">검색 결과가 없습니다.</div>';
			return;
		}
		listEl.innerHTML = dbHtml + (externalHtml
			? `<div class="list-section-label">카카오맵 검색 결과 · 즐겨찾기·일정·리뷰를 시도하면 등록됩니다</div>${externalHtml}`
			: "");

		listEl.querySelectorAll("[data-open-popup]").forEach((el) => {
			el.addEventListener("click", async () => {
				const item = findByKey(el.dataset.openPopup);
				if (!item) return;
				const id = await ensureMaterialized(item);
				if (id) PlacePopup.open(id);
			});
		});
		listEl.querySelectorAll("[data-fav-btn]").forEach((el) => {
			el.addEventListener("click", async () => {
				const item = findByKey(el.dataset.favBtn);
				if (!item) return;
				const id = await ensureMaterialized(item);
				if (!id) return;
				const result = await PlacePopup.toggleFavoriteRaw(id);
				if (result) {
					item.favorited = result.favorited;
					el.textContent = result.favorited ? "♥" : "♡";
					el.classList.toggle("active", result.favorited);
				}
			});
		});
		if (opts.showAddButton) {
			listEl.querySelectorAll("[data-add-btn]").forEach((el) => {
				el.addEventListener("click", async () => {
					const item = findByKey(el.dataset.addBtn);
					if (!item) return;
					const id = await ensureMaterialized(item);
					if (!id) return;
					if (opts.onAddToTrip) opts.onAddToTrip({ ...item, id });
				});
			});
		}
	}

	/**
	 * 카카오 전용 결과는 실제 id 가 없다. 즐겨찾기·일정·리뷰 중 하나를 처음 시도하는
	 * 순간 서버에 Place 로 만들고, 이후로는 이 객체에 박힌 id 를 그대로 쓴다
	 * (같은 세션에서 두 번째 클릭부터는 다시 만들지 않는다).
	 */
	async function ensureMaterialized(item) {
		if (item.id) return item.id;
		if (!PlacePopup.isLoggedIn) {
			if (await UIDialog.confirm("로그인이 필요합니다. 로그인 페이지로 이동할까요?")) {
				location.href = "/login";
			}
			return null;
		}
		try {
			const result = await Api.post("/api/places/from-kakao", {
				kakaoId: item.kakaoId,
				name: item.name,
				address: item.address,
				phone: item.phone,
				latitude: item.latitude,
				longitude: item.longitude,
				category: item.category,
			});
			item.id = result.id;
			return item.id;
		} catch (e) {
			UIDialog.alert(e.message);
			return null;
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

	return { refresh: fetchResults, updateFavoriteBadge, ensureMaterialized, state };
}
