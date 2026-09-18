package com.petcheck.backend.controller.admin;

import com.petcheck.backend.common.properties.ExternalApiProperties;
import com.petcheck.backend.external.tour.TourApiClient;
import com.petcheck.backend.external.tour.dto.TourPetDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 설정이 제대로 주입됐는지 브라우저에서 눈으로 확인하는 용도.
 * 값이 비어 있는지, 파일이 있는지를 로그를 뒤지지 않고 알 수 있다.
 * <p>
 * 키 원문은 절대 그대로 내보내지 않는다. 앞뒤 4자와 길이만 보여준다.
 * 길이만으로도 인코딩 키를 잘못 넣었는지 알 수 있다(인코딩 키가 더 길다).
 * 배포 전에 이 컨트롤러는 삭제하거나 ADMIN 권한을 걸어야 한다.
 */
@RestController
@RequestMapping("/api/admin/diagnostics")
@RequiredArgsConstructor
public class DiagnosticsController {

    private final ExternalApiProperties properties;
    private final TourApiClient tourApiClient;

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> result = new LinkedHashMap<>();

        ExternalApiProperties.TourApi tourApi = properties.tourApi();
        result.put("tourApi.key", mask(tourApi.key()));
        result.put("tourApi.keyForm", tourApi.key() == null || tourApi.key().isBlank()
                ? "(비어 있음)"
                : tourApi.key().contains("%") ? "Encoding" : "Decoding");
        result.put("tourApi.baseUrl", tourApi.baseUrl());
        result.put("tourApi.detailOperation", tourApi.detailOperation());

        String csvPath = properties.cultureData().csvPath();
        Path path = Paths.get(csvPath);
        result.put("cultureData.csvPath", path.toAbsolutePath().toString());
        result.put("cultureData.csvExists", Files.exists(path));
        result.put("cultureData.csvSizeMb", fileSizeMb(path));
        result.put("cultureData.charset", properties.cultureData().charset());

        Path xlsx = Paths.get(properties.foodSafety().xlsxPath());
        result.put("foodSafety.xlsxPath", xlsx.toAbsolutePath().toString());
        result.put("foodSafety.xlsxExists", Files.exists(xlsx));

        // OC 는 이메일 ID 라 짧다. 마스킹하면 오히려 확인이 안 되므로 그대로 보여준다.
        result.put("law.oc", properties.law().oc().isBlank()
                ? "(비어 있음)" : properties.law().oc());

        return result;
    }

    /** TourApiClient 가 실제로 외부 호출에 성공하는지. null 이면 실패한 것이다. */
    @GetMapping("/tour/{contentId}")
    public Map<String, Object> tour(@PathVariable String contentId) {
        TourPetDetail detail = tourApiClient.fetchPetDetail(contentId);

        return Map.of(
                "contentId", contentId,
                "success", detail != null,
                "detail", detail == null ? "(null — 서버 로그의 WARN 을 확인하세요)" : detail
        );
    }

    private String mask(String key) {
        if (key == null || key.isBlank()) {
            return "(비어 있음)";
        }
        if (key.length() <= 8) {
            return "*** (길이 " + key.length() + ")";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4)
                + " (길이 " + key.length() + ")";
    }

    private Object fileSizeMb(Path path) {
        try {
            return Files.exists(path) ? Files.size(path) / 1024 / 1024 : 0;
        } catch (Exception e) {
            return "확인 실패";
        }
    }
}