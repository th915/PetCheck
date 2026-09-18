package com.petcheck.backend.service.trust;

import com.petcheck.backend.common.properties.ReviewScoreProperties;
import com.petcheck.backend.common.properties.TrustScoreProperties;
import com.petcheck.backend.entity.review.AuthenticityStatus;
import com.petcheck.backend.entity.review.VerificationLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 증분 누적이 성립하는지 검증한다.
 * <p>
 * decayBase × factor 가 "직접 계산한 감쇠 가중치"와 같아야 한다는 것이
 * 이 구조 전체의 전제다. 이게 깨지면 리뷰를 다시 읽지 않는 최적화가 무너진다.
 */
class TrustScoreCalculatorTest {

    private static final int HALF_LIFE = 180;

    private final TrustScoreCalculator calculator = new TrustScoreCalculator(
            new TrustScoreProperties(5.0),
            new ReviewScoreProperties(HALF_LIFE, 0.05, 5, 300.0,
                    Map.of(VerificationLevel.RECEIPT, 1.0,
                            VerificationLevel.PHOTO_EXIF, 0.85,
                            VerificationLevel.LOCATION, 0.7,
                            VerificationLevel.SCHEDULE, 0.5,
                            VerificationLevel.NONE, 0.25),
                    Map.of(AuthenticityStatus.GENUINE, 1.0,
                            AuthenticityStatus.PENDING, 0.5,
                            AuthenticityStatus.SUSPICIOUS, 0.3,
                            AuthenticityStatus.FAKE, 0.0)));

    @Test
    @DisplayName("오늘 방문한 영수증 리뷰의 가중치는 1.0 이다")
    void 오늘_영수증리뷰() {
        LocalDateTime now = LocalDateTime.now();

        double weight = calculator.decayBase(VerificationLevel.RECEIPT, now)
                * calculator.decayFactor(now);

        assertThat(weight).isCloseTo(1.0, within(0.0001));
    }

    @Test
    @DisplayName("반감기만큼 지나면 가중치가 절반이 된다")
    void 반감기_경과() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime visited = now.minusDays(HALF_LIFE);

        double weight = calculator.decayBase(VerificationLevel.RECEIPT, visited)
                * calculator.decayFactor(now);

        assertThat(weight).isCloseTo(0.5, within(0.001));
    }

    @Test
    @DisplayName("일반 리뷰 4건이 영수증 리뷰 1건과 같은 무게다")
    void 인증등급_차이() {
        LocalDateTime now = LocalDateTime.now();
        double factor = calculator.decayFactor(now);

        double receipt = calculator.decayBase(VerificationLevel.RECEIPT, now) * factor;
        double none = calculator.decayBase(VerificationLevel.NONE, now) * factor;

        assertThat(receipt / none).isCloseTo(4.0, within(0.0001));
    }

    @Test
    @DisplayName("decayBase 합에 factor 를 곱한 값이 개별 가중치의 합과 같다")
    void 증분누적이_성립한다() {
        LocalDateTime now = LocalDateTime.now();
        double factor = calculator.decayFactor(now);

        // 리뷰 셋: 10일 전 영수증, 200일 전 일반, 30일 전 위치
        double base1 = calculator.decayBase(VerificationLevel.RECEIPT, now.minusDays(10));
        double base2 = calculator.decayBase(VerificationLevel.NONE, now.minusDays(200));
        double base3 = calculator.decayBase(VerificationLevel.LOCATION, now.minusDays(30));

        // Place 가 들고 있는 누적합
        double decaySum = base1 + base2 + base3;

        // 하나씩 계산한 가중치의 합
        double individual = base1 * factor + base2 * factor + base3 * factor;

        assertThat(decaySum * factor).isCloseTo(individual, within(1e-9));
        assertThat(decaySum * factor).isCloseTo(1.7015, within(0.001));
    }
}