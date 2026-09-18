package com.petcheck.backend.controller.favorite;

import com.petcheck.backend.common.security.MemberPrincipal;
import com.petcheck.backend.dto.favorite.FavoriteToggleResponse;
import com.petcheck.backend.service.favorite.FavoriteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 즐겨찾기 토글/조회. 로그인한 회원이면 역할과 무관하게 쓸 수 있다.
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

	private final FavoriteService favoriteService;

	@PostMapping("/{placeId}")
	public FavoriteToggleResponse toggle(@PathVariable Long placeId,
			@AuthenticationPrincipal MemberPrincipal principal) {
		return new FavoriteToggleResponse(favoriteService.toggle(principal.id(), placeId));
	}

	@GetMapping
	public List<Long> myFavorites(@AuthenticationPrincipal MemberPrincipal principal) {
		return favoriteService.findMyPlaceIds(principal.id());
	}
}
