package com.petcheck.backend.service.favorite;

import com.petcheck.backend.common.exception.BusinessException;
import com.petcheck.backend.common.exception.ErrorCode;
import com.petcheck.backend.entity.favorite.Favorite;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.repository.favorite.FavoriteRepository;
import com.petcheck.backend.repository.member.MemberRepository;
import com.petcheck.backend.repository.place.PlaceRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

	private final FavoriteRepository favoriteRepository;
	private final MemberRepository memberRepository;
	private final PlaceRepository placeRepository;

	@Transactional
	public boolean toggle(Long memberId, Long placeId) {
		if (favoriteRepository.existsByMemberIdAndPlaceId(memberId, placeId)) {
			favoriteRepository.deleteByMemberIdAndPlaceId(memberId, placeId);
			return false;
		}

		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		Place place = placeRepository.findById(placeId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

		favoriteRepository.save(Favorite.builder().member(member).place(place).build());
		return true;
	}

	public List<Long> findMyPlaceIds(Long memberId) {
		return favoriteRepository.findAllPlaceIdsByMemberId(memberId);
	}

	public List<Long> findFavoritedAmong(Long memberId, Collection<Long> placeIds) {
		if (memberId == null || placeIds == null || placeIds.isEmpty()) {
			return List.of();
		}
		return favoriteRepository.findFavoritedPlaceIdsRaw(memberId, placeIds);
	}
}
