package com.petcheck.backend.entity.pet;

/**
 * 장소별 안내 문구를 바꾸는 트리거.
 * 자유 입력 문자열이면 문구 분기를 걸 수 없어 열거형으로 고정한다.
 * 여기 없는 특성은 PetProfile.note 에 적는다.
 */
public enum PetTrait {

    /** 짖음이 잦음 → 칸막이석·야외석 우선 안내 */
    BARKS_OFTEN,

    /** 다른 동물과 합사 어려움 → 동반 밀집 장소 경고 */
    NOT_SOCIABLE,

    /** 낯선 사람을 경계함 */
    WARY_OF_STRANGERS,

    /** 이동장 적응 완료 → 이동장 필수 매장도 안내 가능 */
    CARRIER_TRAINED,

    /** 예방접종 완료 → 접종 확인이 필요한 매장 필터 통과 */
    VACCINATED
}
