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
