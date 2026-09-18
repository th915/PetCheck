const Api = {
	async request(method, url, body) {
		const res = await fetch(url, {
			method,
			headers: body !== undefined ? { "Content-Type": "application/json" } : {},
			body: body !== undefined ? JSON.stringify(body) : undefined,
		});
		if (res.status === 204) {
			return null;
		}
		let payload = null;
		try {
			payload = await res.json();
		} catch (e) {
			// no body / not JSON
		}
		if (!res.ok) {
			const message = (payload && payload.message) || `요청에 실패했습니다. (${res.status})`;
			const err = new Error(message);
			err.code = payload && payload.code;
			err.status = res.status;
			err.fieldErrors = payload && payload.fieldErrors;
			throw err;
		}
		return payload;
	},
	get(url) {
		return this.request("GET", url);
	},
	post(url, body) {
		return this.request("POST", url, body === undefined ? {} : body);
	},
	put(url, body) {
		return this.request("PUT", url, body === undefined ? {} : body);
	},
	patch(url, body) {
		return this.request("PATCH", url, body === undefined ? {} : body);
	},
	del(url) {
		return this.request("DELETE", url);
	},
	async upload(url, file, fieldName) {
		const formData = new FormData();
		formData.append(fieldName || "file", file);
		const res = await fetch(url, { method: "POST", body: formData });
		const payload = await res.json().catch(() => null);
		if (!res.ok) {
			const message = (payload && payload.message) || `업로드에 실패했습니다. (${res.status})`;
			throw new Error(message);
		}
		return payload;
	},
};

const CATEGORY_LABELS = {
	RESTAURANT: "식당", CAFE: "카페", ACCOMMODATION: "숙박", CULTURE: "문화시설", TOURIST_SPOT: "관광지",
	PET_SITTER: "펫시터", PET_HOTEL: "애견호텔", PET_KINDERGARTEN: "애견유치원",
	VET: "동물병원", PET_PHARMACY: "동물약국", PET_SUPPLY: "펫샵", PET_GROOMING: "미용",
	ETC: "기타",
};

function categoryLabel(category) {
	return CATEGORY_LABELS[category] || category;
}

function distanceKm(lat1, lng1, lat2, lng2) {
	if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return null;
	const dLat = (lat2 - lat1) * 111.32;
	const dLng = (lng2 - lng1) * 111.32 * Math.cos((lat1 * Math.PI) / 180);
	return Math.sqrt(dLat * dLat + dLng * dLng);
}

function escapeHtml(str) {
	if (str === null || str === undefined) return "";
	return String(str)
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;")
		.replace(/'/g, "&#39;");
}

/**
 * window.confirm/alert 대체.
 * <p>
 * 네이티브 다이얼로그는 임베디드 웹뷰(앱 내 브라우저, 자동화 도구 등)에서
 * 아예 뜨지 않거나 자동으로 취소돼 버리는 환경이 있다. 그런 곳에서는
 * "삭제" 버튼을 눌러도 아무 일도 안 일어나는 것처럼 보인다.
 * 페이지 안에 직접 그리는 모달이면 어떤 브라우저 컨텍스트에서도 동일하게 동작한다.
 */
const UIDialog = (() => {
	function build(message, { showCancel, showInput }) {
		const overlay = document.createElement("div");
		overlay.className = "ui-dialog-overlay";
		overlay.innerHTML = `
			<div class="ui-dialog-box">
				<div class="ui-dialog-message"></div>
				${showInput ? '<input type="text" class="ui-dialog-input">' : ""}
				<div class="ui-dialog-actions">
					${showCancel ? '<button type="button" class="ui-dialog-btn ui-dialog-cancel">취소</button>' : ""}
					<button type="button" class="ui-dialog-btn ui-dialog-ok">확인</button>
				</div>
			</div>
		`;
		overlay.querySelector(".ui-dialog-message").textContent = message;
		document.body.appendChild(overlay);
		return overlay;
	}

	function open(message, { showCancel, showInput }) {
		return new Promise((resolve) => {
			const overlay = build(message, { showCancel, showInput });
			const input = overlay.querySelector(".ui-dialog-input");
			const finish = (result) => {
				overlay.remove();
				resolve(result);
			};
			const ok = () => finish(showInput ? input.value : true);
			overlay.querySelector(".ui-dialog-ok").addEventListener("click", ok);
			const cancelBtn = overlay.querySelector(".ui-dialog-cancel");
			if (cancelBtn) cancelBtn.addEventListener("click", () => finish(showInput ? null : false));
			overlay.addEventListener("click", (e) => {
				if (e.target === overlay) finish(showInput ? null : false);
			});
			if (input) {
				input.addEventListener("keydown", (e) => {
					if (e.key === "Enter") ok();
				});
				input.focus();
			} else {
				overlay.querySelector(".ui-dialog-ok").focus();
			}
		});
	}

	return {
		confirm(message) {
			return open(message, { showCancel: true });
		},
		alert(message) {
			return open(message, { showCancel: false });
		},
		/** 취소하면 null, 확인하면 입력값(빈 문자열 포함)을 준다 */
		prompt(message) {
			return open(message, { showCancel: true, showInput: true });
		},
	};
})();
