package com.petcheck.backend.service.trust;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param processed        점수를 다시 계산한 장소 수
 * @param gradeChanged     등급이 바뀐 장소 수 — "리뷰로 재조정된 장소 수" 지표
 * @param changedPlaceIds  등급이 바뀐 장소의 id. 알림 발송 대상이 된다
 * @param calculatedAt     이번 실행의 기준 시각. 스냅샷이 전부 이 값을 공유한다
 */
public record BatchResult(
        int processed,
        int gradeChanged,
        List<Long> changedPlaceIds,
        LocalDateTime calculatedAt
) {
}