package com.petcheck.backend.external.nts;

/**
 * 국세청 상태조회 결과.
 *
 * @param stateCode 01 계속사업자 / 02 휴업자 / 03 폐업자.
 *                  국세청에 없는 번호면 빈 문자열이 온다
 */
public record BusinessStatus(String number, String stateCode, String stateName, String closedDate) {

    public boolean isActive() {
        return "01".equals(stateCode);
    }

    public boolean isUnregistered() {
        return stateCode == null || stateCode.isBlank();
    }
}