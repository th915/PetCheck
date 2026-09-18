const ReviewWrite = {
	bind(placeId) {
		const slot = document.getElementById("reviewWriteSlot");
		if (!slot) return;

		if (!PlacePopup.isLoggedIn) {
			slot.innerHTML = '<div class="popup-section review-write-hint">로그인하면 리뷰를 작성할 수 있습니다.</div>';
			return;
		}
		if (!PlacePopup.isCustomer) {
			slot.innerHTML = "";
			return;
		}

		const today = new Date().toISOString().substring(0, 10);
		slot.innerHTML = `
			<div class="popup-section review-write-box">
				<button type="button" class="add-to-trip-btn" id="reviewWriteToggle">+ 리뷰 작성</button>
				<form id="reviewWriteForm" style="display:none; margin-top:10px;">
					<div class="form-group">
						<label>방문일</label>
						<input type="date" name="visitedAt" max="${today}" required>
					</div>
					<div class="form-group">
						<label>등록된 정보와 실제가 일치했나요?</label>
						<label class="role-option"><input type="radio" name="matched" value="true" checked> 일치했어요</label>
						<label class="role-option"><input type="radio" name="matched" value="false"> 달랐어요</label>
					</div>
					<div class="form-group" id="mismatchReasonsGroup" style="display:none;">
						<label>무엇이 달랐나요 (복수 선택)</label>
						<label class="role-option"><input type="checkbox" name="mismatchReasons" value="NOT_ALLOWED"> 동반 자체 불가</label>
						<label class="role-option"><input type="checkbox" name="mismatchReasons" value="SIZE_LIMIT_DIFFERENT"> 크기·체중 제한 다름</label>
						<label class="role-option"><input type="checkbox" name="mismatchReasons" value="AREA_CONDITION_DIFFERENT"> 구역 조건 다름</label>
						<label class="role-option"><input type="checkbox" name="mismatchReasons" value="REQUIREMENT_DIFFERENT"> 이동장·목줄 조건 다름</label>
						<label class="role-option"><input type="checkbox" name="mismatchReasons" value="FEE_DIFFERENT"> 추가 요금 다름</label>
						<label class="role-option"><input type="checkbox" name="mismatchReasons" value="CLOSED"> 폐업/휴업</label>
						<label class="role-option"><input type="checkbox" name="mismatchReasons" value="OTHER"> 기타</label>
					</div>
					<div class="form-group">
						<label>후기 내용</label>
						<textarea name="content" rows="3" maxlength="1000" placeholder="방문 경험을 적어주세요"></textarea>
					</div>
					<div class="form-group">
						<label>사진 첨부 (선택, 최대 5장)</label>
						<input type="file" name="images" accept="image/*" multiple>
					</div>
					<div class="form-group">
						<label>영수증 첨부 (선택)</label>
						<input type="file" name="receipt" accept="image/*">
						<div class="field-hint">영수증을 첨부하면 방문 인증 등급이 올라가 리뷰 신뢰도가 높아집니다.</div>
					</div>
					<div class="form-message error" id="reviewWriteError" style="display:none;"></div>
					<button type="submit" class="btn-primary" id="reviewWriteSubmit">리뷰 등록</button>
				</form>
			</div>
		`;

		const toggleBtn = document.getElementById("reviewWriteToggle");
		const form = document.getElementById("reviewWriteForm");
		toggleBtn.addEventListener("click", () => {
			form.style.display = form.style.display === "none" ? "block" : "none";
		});

		form.querySelectorAll('input[name="matched"]').forEach((el) => {
			el.addEventListener("change", () => {
				document.getElementById("mismatchReasonsGroup").style.display =
					form.matched.value === "false" ? "block" : "none";
			});
		});

		form.addEventListener("submit", (e) => {
			e.preventDefault();
			this.submit(placeId, form);
		});
	},

	async submit(placeId, form) {
		const errorEl = document.getElementById("reviewWriteError");
		const submitBtn = document.getElementById("reviewWriteSubmit");
		errorEl.style.display = "none";
		submitBtn.disabled = true;
		submitBtn.textContent = "등록 중...";

		try {
			const formData = new FormData(form);
			const visitedAt = formData.get("visitedAt");
			const matched = formData.get("matched") === "true";
			const mismatchReasons = formData.getAll("mismatchReasons");
			const content = formData.get("content");
			const imageFiles = form.images.files;
			const receiptFile = form.receipt.files[0];

			if (imageFiles.length > 5) {
				throw new Error("사진은 최대 5장까지 첨부할 수 있습니다.");
			}

			const imageUrls = [];
			for (const file of imageFiles) {
				const uploaded = await Api.upload("/api/reviews/images", file);
				imageUrls.push(uploaded.url);
			}

			let receiptImageUrl = null;
			if (receiptFile) {
				const uploaded = await Api.upload("/api/reviews/images", receiptFile);
				receiptImageUrl = uploaded.url;
			}

			const coords = await this.tryGeolocation();

			await Api.post("/api/reviews", {
				placeId: Number(placeId),
				matched,
				mismatchReasons: matched ? [] : mismatchReasons,
				imageUrls,
				visitedAt,
				content,
				latitude: coords ? coords.lat : null,
				longitude: coords ? coords.lng : null,
				receiptImageUrl,
			});

			alert("리뷰가 등록되었습니다. AI 진위 판정과 영수증 인증은 잠시 후 자동으로 반영됩니다.");
			PlacePopup.open(placeId);
		} catch (e) {
			errorEl.textContent = e.message;
			errorEl.style.display = "block";
			submitBtn.disabled = false;
			submitBtn.textContent = "리뷰 등록";
		}
	},

	tryGeolocation() {
		return new Promise((resolve) => {
			if (!navigator.geolocation) {
				resolve(null);
				return;
			}
			navigator.geolocation.getCurrentPosition(
				(pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
				() => resolve(null),
				{ timeout: 3000 }
			);
		});
	},
};
