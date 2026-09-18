package com.petcheck.backend.entity.place;

/**
 * trust_score 를 사용자에게 보여줄 등급으로 끊는다.
 * 점수는 리뷰 한 건에도 소수점이 흔들리지만, 알림은 "등급이 바뀐 장소"에만 보낸다.
 * 이 구분이 없으면 리뷰가 달릴 때마다 알림이 나간다.
 */
public enum TrustGrade {

    VERIFIED(0.85),
    RELIABLE(0.65),
    UNCERTAIN(0.40),
    DOUBTFUL(0.0);

    private final double threshold;

    TrustGrade(double threshold) {
        this.threshold = threshold;
    }

    public double threshold() {
        return threshold;
    }

    public static TrustGrade of(double score) {
        for (TrustGrade grade : values()) {
            if (score >= grade.threshold) {
                return grade;
            }
        }
        return DOUBTFUL;
    }
}
