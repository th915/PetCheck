package com.petcheck.backend.external.tour.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TourAPI 반려동물 동반여행 상세 응답 한 건.
 * <p>
 * ignoreUnknown = true 가 필수다. 공공데이터 응답은 예고 없이 필드가 늘어나는데,
 * 기본 설정이면 모르는 필드 하나에 전체 파싱이 예외로 죽는다.
 * <p>
 * 아래 필드명은 확인되지 않았다. 실제 응답을 한 번 받아보고(http/tour-api.http)
 * 다른 이름이면 이 record 만 고치면 된다 — 나머지 코드는 PlaceSyncData 만 본다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourPetDetail(
        String contentid,
        String acmpyTypeCd,        // 동반 가능 유형
        String acmpyPsblCpam,      // 동반 가능 반려동물
        String acmpyNeedMtr,       // 동반 시 필요사항
        String relaAcdntRiskMtr,   // 사고 대비 사항
        String etcAcmpyInfo        // 기타 동반 정보
) {
}