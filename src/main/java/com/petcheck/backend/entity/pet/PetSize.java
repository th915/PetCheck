package com.petcheck.backend.entity.pet;

/**
 * 입장 가능 동물 크기 제한(문화정보원 데이터 필드)과 대응시킨다.
 * 체중 경계는 업계 통용 기준이며, 매장이 별도 kg 상한을 걸면 PetPolicy 쪽이 우선한다.
 */
public enum PetSize {

    SMALL(10.0),
    MEDIUM(25.0),
    LARGE(Double.MAX_VALUE);

    private final double maxWeightKg;

    PetSize(double maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public static PetSize fromWeight(double weightKg) {
        for (PetSize size : values()) {
            if (weightKg <= size.maxWeightKg) {
                return size;
            }
        }
        return LARGE;
    }
}
