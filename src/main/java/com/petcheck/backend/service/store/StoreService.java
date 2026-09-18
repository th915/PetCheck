package com.petcheck.backend.service.store;

import com.petcheck.backend.common.exception.BusinessException;
import com.petcheck.backend.common.exception.ErrorCode;
import com.petcheck.backend.common.storage.LocalImageStorage;
import com.petcheck.backend.dto.store.*;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.SourceType;
import com.petcheck.backend.entity.store.Store;
import com.petcheck.backend.entity.store.VerificationStatus;
import com.petcheck.backend.external.nts.BusinessRegistryClient;
import com.petcheck.backend.repository.member.MemberRepository;
import com.petcheck.backend.repository.place.PlaceRepository;
import com.petcheck.backend.repository.store.StoreRepository;
import com.petcheck.backend.service.trust.TrustScoreCalculator;
import com.petcheck.backend.external.nts.BusinessRegistryClient;
import com.petcheck.backend.external.nts.BusinessStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;
    private final LocalImageStorage localImageStorage;
    private final TrustScoreCalculator trustScoreCalculator;   // TrustScoreProperties 대신
    private final BusinessRegistryClient businessRegistryClient;



    // --- 매장(사장님) ---

    @Transactional
    public StoreResponse register(Long ownerId, StoreRegisterRequest request) {
        if ((request.placeId() == null) == (request.newPlace() == null)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "기존 장소를 선택하거나 새 매장 정보를 입력해주세요. 둘 중 하나만 보내야 합니다");
        }

        String businessNumber = verifyBusinessNumber(request.businessNumber());

        Member owner = memberRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Place place = resolvePlace(request);

        // 사장님은 여러 매장을 가질 수 있다. 막는 건 "한 장소에 매장 둘" 뿐이다.
        if (storeRepository.existsByPlaceId(place.getId())) {
            throw new BusinessException(ErrorCode.STORE_ALREADY_REGISTERED);
        }

        Store store = storeRepository.save(Store.builder()
                .place(place)
                .owner(owner)
                .businessName(request.businessName())
                .businessNumber(businessNumber)
                .legalChecklist(request.legalChecklist().toEntity())
                .proofImageUrl(request.proofImageUrl())
                .build());

        place.attachStore(store);

        if (request.petPolicy() != null) {
            place.applyStorePolicy(request.petPolicy().toEntity());
        }

        return StoreResponse.from(store);
    }

    /** 사장님의 매장 목록. 없으면 빈 배열이지 에러가 아니다 */
    public List<StoreResponse> findMyStores(Long ownerId) {
        return storeRepository.findAllByOwnerId(ownerId).stream()
                .map(StoreResponse::from)
                .toList();
    }

    /**
     * 기존 장소를 찾아오거나, 사장님이 보낸 정보로 새 장소를 만든다.
     * 새로 만든 장소는 공공데이터 출처가 아니므로 publiclyRegistered = false 이고
     * 사전 점수 0.5 에서 시작한다. 관리자 승인을 받아야 0.8 로 올라간다.
     */
    private Place resolvePlace(StoreRegisterRequest request) {
        if (request.placeId() != null) {
            return placeRepository.findById(request.placeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        }

        if (request.petPolicy() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "새 매장을 등록할 때는 반려동물 동반 조건도 함께 보내야 합니다");
        }

        NewPlaceRequest newPlace = request.newPlace();
        checkNameNotDuplicated(newPlace);
        String externalId = externalIdOf(newPlace);

        // 같은 이름·좌표로 만든 장소가 이미 있으면 재사용한다.
        // 등록을 두 번 시도했을 때 같은 가게가 두 개 생기는 것을 막는다.
        return placeRepository.findBySourceTypeAndExternalId(SourceType.STORE_SUBMITTED, externalId)
                .orElseGet(() -> placeRepository.save(Place.builder()
                        .sourceType(SourceType.STORE_SUBMITTED)
                        .externalId(externalId)
                        .publiclyRegistered(false)
                        .name(newPlace.name())
                        .category(newPlace.category())
                        .address(newPlace.address())
                        .phone(newPlace.phone())
                        .latitude(newPlace.latitude())
                        .longitude(newPlace.longitude())
                        .petPolicy(request.petPolicy().toEntity())
                        .build()));
    }

    /** 반경 500m 안에 같은 이름의 장소가 있으면 막는다 */
    private void checkNameNotDuplicated(NewPlaceRequest request) {
        double latDelta = 0.0045;                                        // 위도 500m
        double lngDelta = 0.0045 / Math.cos(Math.toRadians(request.latitude()));

        long found = placeRepository.countSimilarNamedNearby(
                request.name().replaceAll("[^가-힣a-zA-Z0-9]", "").toLowerCase(),
                request.latitude() - latDelta, request.latitude() + latDelta,
                request.longitude() - lngDelta, request.longitude() + lngDelta);

        if (found > 0) {
            throw new BusinessException(ErrorCode.STORE_NAME_DUPLICATED,
                    "반경 500m 안에 같은 이름의 장소가 이미 있습니다. 기존 장소를 선택해 등록해주세요");
        }
    }

    /** external_id 는 VARCHAR(100) 이라 이름을 60자로 자른다 */
    private String externalIdOf(NewPlaceRequest request) {
        String name = request.name().length() > 60
                ? request.name().substring(0, 60)
                : request.name();

        return String.format(Locale.ROOT, "%s|%.5f|%.5f",
                name, request.latitude(), request.longitude());
    }

    /** 체크리스트를 고치면 승인이 풀린다. Store.updateChecklist() 안에서 처리된다 */
    @Transactional
    public StoreResponse updateChecklist(Long ownerId, Long storeId,
                                         LegalChecklistRequest request, String proofImageUrl) {
        Store store = findOwnedStore(ownerId, storeId);

        store.updateChecklist(request.toEntity(), proofImageUrl);

        // 인증이 풀렸으므로 사전 점수가 0.8 에서 내려간다. 바로 반영한다.
        store.getPlace().attachStore(store);
        trustScoreCalculator.refresh(store.getPlace());

        return StoreResponse.from(store);
    }

    @Transactional
    public void deleteStore(Long ownerId, Long storeId) {
        detachAndDelete(findOwnedStore(ownerId, storeId));
    }

    /** 회원 탈퇴 시 그 사장님의 매장을 전부 정리한다. 없으면 아무 일도 하지 않는다 */
    @Transactional
    public void deleteAllByOwner(Long ownerId) {
        storeRepository.findAllByOwnerId(ownerId).forEach(this::detachAndDelete);
    }

    /**
     * 남의 매장을 건드리지 못하게 막는다.
     * <p>
     * owner 는 지연 로딩이지만 getId() 는 프록시가 이미 들고 있는 값이라
     * 이 비교 하나로는 추가 쿼리가 나가지 않는다.
     */
    private Store findOwnedStore(Long ownerId, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        if (!store.getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NOT_STORE_OWNER);
        }
        return store;
    }

    /**
     * Store 를 지우면서 Place 의 사전 점수를 0.5 로 되돌린다.
     * 메모리상의 연관도 끊어야 한다. 끊지 않으면 이미 지운 매장을 근거로 0.8 이 유지된다.
     */
    private void detachAndDelete(Store store) {
        Place place = store.getPlace();
        storeRepository.delete(store);

        place.attachStore(null);
        trustScoreCalculator.refresh(store.getPlace());

        log.info("매장 삭제 — storeId={}, placeId={}, 신뢰도 {} 로 복귀",
                store.getId(), place.getId(), place.getTrustScore());
    }

    public ImageUploadResponse uploadProofImage(MultipartFile file) {
        return new ImageUploadResponse(localImageStorage.save(file));
    }

    // --- 관리자 ---

    public Page<StoreResponse> findByStatus(VerificationStatus status, Pageable pageable) {
        return storeRepository.findAllByVerificationStatus(status, pageable)
                .map(StoreResponse::from);
    }

    /**
     * 승인하면 법정 기준 인증 배지가 붙고 사전 점수가 0.8 로 올라간다.
     * 배치를 기다리지 않고 그 장소만 즉시 재계산해서, 승인 효과가 바로 보이게 한다.
     */
    @Transactional
    public StoreResponse approve(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        store.approve();

        Place place = store.getPlace();
        place.attachStore(store);
        trustScoreCalculator.refresh(store.getPlace());

        log.info("매장 승인 — storeId={}, placeId={}, 신뢰도 {} ({})",
                storeId, place.getId(), place.getTrustScore(), place.getTrustGrade());

        return StoreResponse.from(store);
    }

    @Transactional
    public StoreResponse reject(Long storeId, String reason) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        store.reject(reason);

        store.getPlace().attachStore(store);
        trustScoreCalculator.refresh(store.getPlace());

        return StoreResponse.from(store);
    }

    /** 하이픈을 걷어낸 사업자번호를 돌려준다. 형식·중복·영업상태를 모두 통과해야 한다 */
    private String verifyBusinessNumber(String raw) {
        String normalized = BusinessRegistryClient.normalize(raw);

        if (!BusinessRegistryClient.hasValidFormat(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_NUMBER_INVALID,
                    "존재할 수 없는 사업자등록번호입니다");
        }
        if (storeRepository.existsByBusinessNumber(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_NUMBER_DUPLICATED);
        }

        BusinessStatus status = businessRegistryClient.fetchStatus(normalized);

        // 조회 실패는 국세청 쪽 문제일 수 있다. 등록은 통과시키고 관리자 승인에서 걸러낸다
        if (status == null) {
            log.warn("사업자 상태를 확인하지 못해 통과시킨다 — {}", normalized);
            return normalized;
        }
        if (status.isUnregistered()) {
            throw new BusinessException(ErrorCode.BUSINESS_NUMBER_INVALID,
                    "국세청에 등록되지 않은 사업자등록번호입니다");
        }
        if (!status.isActive()) {
            throw new BusinessException(ErrorCode.BUSINESS_NOT_ACTIVE,
                    status.stateName() + " 상태로 조회되었습니다");
        }

        log.info("사업자 확인 — {} / {}", normalized, status.stateName());
        return normalized;
    }
}