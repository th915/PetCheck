package com.petcheck.backend.service.review;

import com.petcheck.backend.common.properties.ReviewScoreProperties;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.review.VerificationLevel;
import com.petcheck.backend.repository.schedule.VisitScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 방문을 얼마나 확실하게 증명했는지 판정한다.
 * 여러 조건을 만족하면 가장 높은 등급을 준다 — 그래서 위에서부터 검사한다.
 * <p>
 * 1단계에서는 LOCATION / SCHEDULE / NONE 만 판정한다.
 * RECEIPT 와 PHOTO_EXIF 는 Gemini·EXIF 연동이 붙는 다음 단계에서 이 앞에 끼워 넣는다.
 */
@Service
@RequiredArgsConstructor
public class VisitVerificationService {

    /** 미터 → 위도 1도의 대략적인 거리 */
    private static final double METERS_PER_DEGREE = 111_320.0;

    private final VisitScheduleRepository visitScheduleRepository;
    private final ReviewScoreProperties properties;

    public VerificationLevel determine(Long memberId, Place place, LocalDateTime visitedAt,
                                       Double latitude, Double longitude) {

        if (isWithinRadius(place, latitude, longitude)) {
            return VerificationLevel.LOCATION;
        }
        if (hasPriorSchedule(memberId, place.getId(), visitedAt)) {
            return VerificationLevel.SCHEDULE;
        }
        return VerificationLevel.NONE;
    }

    /**
     * 좌표가 장소 반경 안인지. 정렬이 아니라 판정이므로 제곱근까지 구한다.
     * 경도 1도의 실제 거리는 위도에 따라 줄어들어 cos 보정을 넣는다.
     */
    private boolean isWithinRadius(Place place, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        double dLat = (latitude - place.getLatitude()) * METERS_PER_DEGREE;
        double dLng = (longitude - place.getLongitude())
                * METERS_PER_DEGREE * Math.cos(Math.toRadians(place.getLatitude()));

        return Math.sqrt(dLat * dLat + dLng * dLng) <= properties.locationRadiusMeters();
    }

    /**
     * 방문일 전후 하루 안에 일정이 있었는지.
     * 리포지토리 쿼리가 "일정을 방문 전에 만들었을 것"까지 확인하므로
     * 다녀온 뒤에 일정을 급조해 인증을 받는 우회가 막힌다.
     */
    private boolean hasPriorSchedule(Long memberId, Long placeId, LocalDateTime visitedAt) {
        return visitScheduleRepository.existsPriorSchedule(
                memberId, placeId, visitedAt.minusDays(1), visitedAt.plusDays(1));
    }
}