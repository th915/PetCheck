package com.petcheck.backend.dto.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 매장 등록은 두 경로가 있다.
 * <ul>
 *   <li>placeId — 공공데이터에 이미 있는 장소에 사장님이 자기 매장을 연결한다</li>
 *   <li>newPlace — 공공데이터에 없는 곳(신규 개업, 갱신 누락)을 직접 등록한다</li>
 * </ul>
 * 둘 중 하나만 보내야 한다. 공공데이터에 실릴 때까지 기다려야 등록할 수 있다면
 * 새로 연 가게는 이 서비스를 쓸 방법이 없다.
 *
 * @param proofImageUrl POST /api/store/proof-image 로 먼저 올리고 받은 경로.
 *                      이미지와 JSON 을 한 요청에 섞으면 multipart 조립이 복잡해져서 분리했다
 * @param petPolicy     기존 장소에 연결할 때는 선택(보내면 공공데이터 값을 덮는다).
 *                      newPlace 로 등록할 때는 기준이 될 값이 없으므로 필수다
 */
public record StoreRegisterRequest(

        Long placeId,

        @Valid
        NewPlaceRequest newPlace,

        @NotBlank(message = "상호명은 필수입니다")
        @Size(max = 200)
        String businessName,

        @NotBlank(message = "사업자등록번호는 필수입니다")
        @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$",
                message = "사업자등록번호 형식이 올바르지 않습니다 (10자리)")
        String businessNumber,

        @NotNull(message = "법정 기준 체크리스트는 필수입니다")
        @Valid
        LegalChecklistRequest legalChecklist,

        String proofImageUrl,

        @Valid
        PetPolicyRequest petPolicy
) {
}