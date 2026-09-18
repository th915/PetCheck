package com.petcheck.backend.service.review;

import com.petcheck.backend.dto.review.AuthenticityVerdict;
import com.petcheck.backend.dto.review.ReviewInspection;
import com.petcheck.backend.entity.review.AuthenticityStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAuthenticityService {

    private final ReceiptVerificationService receiptVerificationService;
    private final RuleBasedSpamFilter spamFilter;
    private final GeminiClient geminiClient;
    private final ReviewService reviewService;

    @Value("${app.review.spam-threshold:60}")
    private int spamThreshold;

    /**
     * 커밋이 끝난 뒤에 돈다. 커밋 전에 비동기 스레드가 뜨면
     * 그 스레드의 새 트랜잭션에서는 아직 리뷰가 보이지 않는다.
     */
    @Async("reviewJudgeExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewWritten(ReviewWrittenEvent event) {
        // 영수증 인증을 먼저 끝낸다. 두 작업이 같은 리뷰 행을 동시에 고치면
        // 한쪽이 다른 쪽의 변경을 덮어쓸 수 있어 순서를 고정했다.
        receiptVerificationService.verify(event.reviewId());
        judge(event.reviewId());
    }

    public void judge(Long reviewId) {
        ReviewInspection inspection = spamFilter.inspect(reviewId);
        if (inspection == null) {
            return;
        }

        // 규칙만으로 충분히 명백하면 LLM 을 부르지 않는다
        if (inspection.ruleScore() >= spamThreshold) {
            log.info("규칙 판정으로 FAKE 확정 — reviewId={}, 점수={}, {}",
                    reviewId, inspection.ruleScore(), inspection.ruleReason());
            reviewService.applyAuthenticity(reviewId, AuthenticityStatus.FAKE,
                    inspection.ruleScore(), inspection.ruleReason());
            return;
        }

        // 본문이 없으면 판정할 내용 자체가 없다
        if (inspection.content().isBlank()) {
            reviewService.applyAuthenticity(reviewId, AuthenticityStatus.GENUINE,
                    inspection.ruleScore(), "본문 없음 — 규칙 검사만 통과");
            return;
        }

        AuthenticityVerdict verdict = geminiClient.judge(inspection);

        log.info("AI 판정 — reviewId={}, 결과={}, 규칙점수={}, 사유={}",
                reviewId, verdict.status(), inspection.ruleScore(), verdict.reason());

        reviewService.applyAuthenticity(reviewId, verdict.status(),
                inspection.ruleScore(), verdict.reason());
    }
}