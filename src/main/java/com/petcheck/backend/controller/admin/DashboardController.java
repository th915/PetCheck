package com.petcheck.backend.controller.admin;

import com.petcheck.backend.dto.review.MismatchReasonCount;
import com.petcheck.backend.dto.trust.AccuracyDashboard;
import com.petcheck.backend.service.trust.AccuracyDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AccuracyDashboardService accuracyDashboardService;

    /** 예) /api/admin/dashboard/accuracy?days=30 */
    @GetMapping("/accuracy")
    public AccuracyDashboard accuracy(@RequestParam(defaultValue = "30") int days) {
        return accuracyDashboardService.getDashboard(days);
    }

    @GetMapping("/mismatch-reasons")
    public List<MismatchReasonCount> mismatchReasons(@RequestParam(defaultValue = "30") int days) {
        return accuracyDashboardService.mismatchReasons(days);
    }
}