const MapKit = {
	map: null,
	markers: [],
	polylines: [],
	ready: false,

	load(kakaoJsKey) {
		return new Promise((resolve, reject) => {
			if (!kakaoJsKey) {
				reject(new Error("no-key"));
				return;
			}
			if (window.kakao && window.kakao.maps) {
				resolve();
				return;
			}
			const script = document.createElement("script");
			script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(kakaoJsKey)}&autoload=false`;
			script.onload = () => window.kakao.maps.load(resolve);
			script.onerror = () => reject(new Error("sdk-load-failed"));
			document.head.appendChild(script);
		});
	},

	async init(containerId, kakaoJsKey, center) {
		const fallback = document.getElementById(containerId + "Fallback");
		try {
			await this.load(kakaoJsKey);
			const container = document.getElementById(containerId);
			const centerCoords = center || { lat: 35.1595, lng: 126.8526 };
			this.map = new kakao.maps.Map(container, {
				center: new kakao.maps.LatLng(centerCoords.lat, centerCoords.lng),
				level: 6,
			});
			this.ready = true;
			if (fallback) fallback.style.display = "none";
			return this.map;
		} catch (e) {
			this.ready = false;
			if (fallback) {
				fallback.style.display = "flex";
				fallback.textContent = !kakaoJsKey
					? "카카오맵 API 키가 설정되지 않았습니다. (환경변수 KAKAO_MAP_JS_KEY)"
					: "지도를 불러오지 못했습니다.";
			}
			return null;
		}
	},

	clearMarkers() {
		this.markers.forEach((m) => m.setMap(null));
		this.markers = [];
	},

	clearPolylines() {
		this.polylines.forEach((l) => l.setMap(null));
		this.polylines = [];
	},

	clearAll() {
		this.clearMarkers();
		this.clearPolylines();
	},

	addMarker(lat, lng, label, onClick) {
		if (!this.ready) return null;
		const position = new kakao.maps.LatLng(lat, lng);
		const marker = new kakao.maps.Marker({ position, map: this.map });
		if (label) {
			const overlay = new kakao.maps.CustomOverlay({
				position,
				content: `<div style="background:#ff7a59;color:#fff;font-size:11px;font-weight:700;border-radius:10px;padding:2px 6px;transform:translateY(-32px);white-space:nowrap;">${label}</div>`,
			});
			overlay.setMap(this.map);
			this.markers.push(overlay);
		}
		if (onClick) {
			kakao.maps.event.addListener(marker, "click", onClick);
		}
		this.markers.push(marker);
		return marker;
	},

	drawPolyline(points, color) {
		if (!this.ready || points.length < 2) return;
		const path = points.map((p) => new kakao.maps.LatLng(p.lat, p.lng));
		const line = new kakao.maps.Polyline({
			path,
			strokeWeight: 3,
			strokeColor: color || "#d64545",
			strokeOpacity: 0.9,
			strokeStyle: "solid",
		});
		line.setMap(this.map);
		this.polylines.push(line);
	},

	fitBounds(points) {
		if (!this.ready || points.length === 0) return;
		const bounds = new kakao.maps.LatLngBounds();
		points.forEach((p) => bounds.extend(new kakao.maps.LatLng(p.lat, p.lng)));
		this.map.setBounds(bounds);
	},

	panTo(lat, lng) {
		if (!this.ready) return;
		this.map.panTo(new kakao.maps.LatLng(lat, lng));
	},
};
