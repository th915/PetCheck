package com.petcheck.backend.dto.store;

import com.petcheck.backend.entity.place.PetPolicy;
import jakarta.validation.constraints.Size;

/**
 * 매장이 직접 입력하는 동반 조건. 공공데이터에서 파싱한 값을 덮어쓴다.
 * <p>
 * 모든 필드가 null 을 허용한다. 매장이 모르는 항목은 비워둘 수 있어야 하고,
 * 비운 항목은 "정보 없음"으로 남는다. false 로 바꿔 저장하면 안 된다.
 */
public record PetPolicyRequest(
        Boolean petAllowed,
        Boolean allowSmall,
        Boolean allowMedium,
        Boolean allowLarge,
        Double weightLimitKg,
        Boolean indoorAllowed,
        Boolean outdoorAllowed,
        Boolean carrierRequired,
        Boolean leashRequired,
        Boolean vaccinationRequired,
        Integer extraFee,

        @Size(max = 500, message = "제한사항은 500자 이하여야 합니다")
        String restrictionNote
) {

    public PetPolicy toEntity() {
        return PetPolicy.builder()
                .petAllowed(petAllowed)
                .allowSmall(allowSmall)
                .allowMedium(allowMedium)
                .allowLarge(allowLarge)
                .weightLimitKg(weightLimitKg)
                .indoorAllowed(indoorAllowed)
                .outdoorAllowed(outdoorAllowed)
                .carrierRequired(carrierRequired)
                .leashRequired(leashRequired)
                .vaccinationRequired(vaccinationRequired)
                .extraFee(extraFee)
                .restrictionNote(restrictionNote)
                .build();
    }
}