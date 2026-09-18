package com.petcheck.backend.dto.store;

import com.petcheck.backend.entity.store.LegalChecklist;
import jakarta.validation.constraints.NotNull;

/**
 * 2026년 3월 1일 시행 식품위생법 시행규칙의 반려동물 동반 출입 시설기준 8개 항목.
 * <p>
 * boolean 이 아니라 Boolean + @NotNull 인 이유: primitive 로 두면 값을 안 보낸 항목이
 * 조용히 false 가 된다. "충족 안 함"과 "답을 안 함"은 다르고,
 * 법정 기준 인증의 근거가 되는 입력이라 빠짐없이 답하게 강제해야 한다.
 */
public record LegalChecklistRequest(

        @NotNull(message = "칸막이 등 구역 분리 여부를 선택해주세요")
        Boolean partitioned,

        @NotNull(message = "전용 케이지 비치 여부를 선택해주세요")
        Boolean cageProvided,

        @NotNull(message = "목줄 고정장치 설치 여부를 선택해주세요")
        Boolean leashAnchor,

        @NotNull(message = "테이블·의자 덮개 비치 여부를 선택해주세요")
        Boolean seatCover,

        @NotNull(message = "전용 식기 비치 여부를 선택해주세요")
        Boolean petTableware,

        @NotNull(message = "전용 쓰레기통 비치 여부를 선택해주세요")
        Boolean petWasteBin,

        @NotNull(message = "출입구 표시 여부를 선택해주세요")
        Boolean entranceSign,

        @NotNull(message = "예방접종 확인 절차 운영 여부를 선택해주세요")
        Boolean vaccinationCheck
) {

    public LegalChecklist toEntity() {
        return LegalChecklist.builder()
                .partitioned(partitioned)
                .cageProvided(cageProvided)
                .leashAnchor(leashAnchor)
                .seatCover(seatCover)
                .petTableware(petTableware)
                .petWasteBin(petWasteBin)
                .entranceSign(entranceSign)
                .vaccinationCheck(vaccinationCheck)
                .build();
    }
}