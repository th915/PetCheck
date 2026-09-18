(function () {
	const KEYS = ["partitioned", "cageProvided", "leashAnchor", "seatCover",
		"petTableware", "petWasteBin", "entranceSign", "vaccinationCheck"];

	async function load() {
		try {
			const stores = await Api.get("/api/store/me");
			const store = stores.find((s) => s.id === STORE_ID);
			if (!store) {
				document.getElementById("checklistError").style.display = "block";
				document.getElementById("checklistError").textContent = "매장을 찾을 수 없습니다.";
				return;
			}
			if (store.legalChecklist) {
				KEYS.forEach((key) => {
					const value = store.legalChecklist[key];
					const radio = document.querySelector(`input[name="${key}"][value="${value}"]`);
					if (radio) radio.checked = true;
				});
			}
			if (store.proofImageUrl) {
				document.getElementById("currentProof").innerHTML =
					`<img class="proof-thumb" src="${store.proofImageUrl}" alt="증빙 사진">`;
			}
		} catch (e) {
			alert(e.message);
		}
	}

	function buildChecklist() {
		const result = {};
		for (const key of KEYS) {
			const checked = document.querySelector(`input[name="${key}"]:checked`);
			if (!checked) throw new Error("체크리스트 8개 항목을 모두 선택해주세요.");
			result[key] = checked.value === "true";
		}
		return result;
	}

	document.getElementById("checklistSubmit").addEventListener("click", async () => {
		const errorEl = document.getElementById("checklistError");
		errorEl.style.display = "none";
		const submitBtn = document.getElementById("checklistSubmit");
		try {
			const legalChecklist = buildChecklist();
			submitBtn.disabled = true;
			submitBtn.textContent = "저장 중...";

			let proofImageUrl = null;
			const proofFile = document.getElementById("proofImage").files[0];
			if (proofFile) {
				const uploaded = await Api.upload("/api/store/proof-image", proofFile);
				proofImageUrl = uploaded.url;
			}

			const query = proofImageUrl ? `?proofImageUrl=${encodeURIComponent(proofImageUrl)}` : "";
			await Api.put(`/api/store/${STORE_ID}/checklist${query}`, legalChecklist);
			location.href = "/store/dashboard";
		} catch (e) {
			errorEl.textContent = e.message;
			errorEl.style.display = "block";
			submitBtn.disabled = false;
			submitBtn.textContent = "저장";
		}
	});

	load();
})();
