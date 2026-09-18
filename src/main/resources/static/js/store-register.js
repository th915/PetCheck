(function () {
	let mode = "existing";
	let selectedPlace = null;

	const existingSection = document.getElementById("existingPlaceSection");
	const newSection = document.getElementById("newPlaceSection");
	const modeExistingBtn = document.getElementById("modeExisting");
	const modeNewBtn = document.getElementById("modeNew");
	const selectedInfo = document.getElementById("selectedPlaceInfo");

	function setMode(next) {
		mode = next;
		modeExistingBtn.classList.toggle("active", mode === "existing");
		modeNewBtn.classList.toggle("active", mode === "new");
		existingSection.style.display = mode === "existing" ? "block" : "none";
		newSection.style.display = mode === "new" ? "block" : "none";
	}

	modeExistingBtn.addEventListener("click", () => setMode("existing"));
	modeNewBtn.addEventListener("click", () => setMode("new"));

	createPlaceSearchPanel({
		keywordInputId: "keywordInput",
		listId: "placeList",
		sortBarId: "sortBar",
		showAddButton: true,
		addButtonLabel: "이 장소 선택",
		onAddToTrip: (place) => {
			selectedPlace = place;
			selectedInfo.style.display = "block";
			selectedInfo.textContent = `선택된 장소: ${place.name} (${place.address})`;
		},
	});

	function checklistValue(key) {
		const checked = document.querySelector(`input[name="${key}"]:checked`);
		return checked ? checked.value === "true" : null;
	}

	function buildChecklist() {
		const keys = ["partitioned", "cageProvided", "leashAnchor", "seatCover",
			"petTableware", "petWasteBin", "entranceSign", "vaccinationCheck"];
		const result = {};
		for (const key of keys) {
			const value = checklistValue(key);
			if (value === null) {
				throw new Error("법정 기준 체크리스트 8개 항목을 모두 선택해주세요.");
			}
			result[key] = value;
		}
		return result;
	}

	function buildPetPolicy() {
		return {
			allowSmall: document.getElementById("ppAllowSmall").checked,
			allowMedium: document.getElementById("ppAllowMedium").checked,
			allowLarge: document.getElementById("ppAllowLarge").checked,
			indoorAllowed: document.getElementById("ppIndoor").checked,
			outdoorAllowed: document.getElementById("ppOutdoor").checked,
			leashRequired: document.getElementById("ppLeash").checked,
			carrierRequired: document.getElementById("ppCarrier").checked,
			vaccinationRequired: document.getElementById("ppVaccination").checked,
			weightLimitKg: document.getElementById("ppWeightLimit").value
				? Number(document.getElementById("ppWeightLimit").value) : null,
			extraFee: document.getElementById("ppExtraFee").value
				? Number(document.getElementById("ppExtraFee").value) : null,
			restrictionNote: document.getElementById("ppRestrictionNote").value || null,
		};
	}

	document.getElementById("registerSubmit").addEventListener("click", async () => {
		const errorEl = document.getElementById("registerError");
		errorEl.style.display = "none";
		const submitBtn = document.getElementById("registerSubmit");

		try {
			const legalChecklist = buildChecklist();
			const businessName = document.getElementById("businessName").value.trim();
			const businessNumber = document.getElementById("businessNumber").value.trim();
			if (!businessName) throw new Error("상호명을 입력해주세요.");
			if (!businessNumber) throw new Error("사업자등록번호를 입력해주세요.");

			let placeId = null;
			let newPlace = null;
			let petPolicy = null;

			if (mode === "existing") {
				if (!selectedPlace) throw new Error("연결할 장소를 검색해서 선택해주세요.");
				placeId = selectedPlace.id;
			} else {
				const name = document.getElementById("npName").value.trim();
				const category = document.getElementById("npCategory").value;
				const address = document.getElementById("npAddress").value.trim();
				const phone = document.getElementById("npPhone").value.trim() || null;
				const latitude = Number(document.getElementById("npLat").value);
				const longitude = Number(document.getElementById("npLng").value);
				if (!name || !address || !latitude || !longitude) {
					throw new Error("신규 매장의 이름·주소·위도·경도를 모두 입력해주세요.");
				}
				newPlace = { name, category, address, phone, latitude, longitude };
				petPolicy = buildPetPolicy();
			}

			submitBtn.disabled = true;
			submitBtn.textContent = "처리 중...";

			let proofImageUrl = null;
			const proofFile = document.getElementById("proofImage").files[0];
			if (proofFile) {
				const uploaded = await Api.upload("/api/store/proof-image", proofFile);
				proofImageUrl = uploaded.url;
			}

			await Api.post("/api/store", {
				placeId,
				newPlace,
				businessName,
				businessNumber,
				legalChecklist,
				proofImageUrl,
				petPolicy,
			});

			location.href = "/store/dashboard";
		} catch (e) {
			errorEl.textContent = e.message;
			errorEl.style.display = "block";
			submitBtn.disabled = false;
			submitBtn.textContent = "등록 신청";
		}
	});
})();
