package com.petcheck.backend.controller.store;

import com.petcheck.backend.common.security.MemberPrincipal;
import com.petcheck.backend.dto.store.ImageUploadResponse;
import com.petcheck.backend.dto.store.LegalChecklistRequest;
import com.petcheck.backend.dto.store.StoreRegisterRequest;
import com.petcheck.backend.dto.store.StoreResponse;
import com.petcheck.backend.service.store.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 사장님용. SecurityConfig 에서 /api/store/** 는 STORE 역할만 접근한다.
 * 컨트롤러마다 권한을 적지 않고 경로 규칙 한 곳에 모아둔 이유는,
 * 경로가 늘어날 때 빠뜨리는 실수를 막기 위해서다.
 */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /** 증빙 이미지를 먼저 올리고, 반환된 url 을 등록 요청에 넣는다 */
    @PostMapping(value = "/proof-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadProofImage(@RequestPart("file") MultipartFile file) {
        return storeService.uploadProofImage(file);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse register(@AuthenticationPrincipal MemberPrincipal principal,
                                  @Valid @RequestBody StoreRegisterRequest request) {
        return storeService.register(principal.id(), request);
    }

    /** 내 매장 목록. 사장님은 여러 매장을 가질 수 있다 */
    @GetMapping("/me")
    public List<StoreResponse> myStores(@AuthenticationPrincipal MemberPrincipal principal) {
        return storeService.findMyStores(principal.id());
    }

    /** 체크리스트 수정. 승인이 풀리고 다시 심사를 받는다 */
    @PutMapping("/{storeId}/checklist")
    public StoreResponse updateChecklist(@AuthenticationPrincipal MemberPrincipal principal,
                                         @PathVariable Long storeId,
                                         @Valid @RequestBody LegalChecklistRequest request,
                                         @RequestParam(required = false) String proofImageUrl) {
        return storeService.updateChecklist(principal.id(), storeId, request, proofImageUrl);
    }

    /** 매장 등록 취소. 그 장소의 신뢰도는 공공데이터 기준(0.5)으로 돌아간다 */
    @DeleteMapping("/{storeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStore(@AuthenticationPrincipal MemberPrincipal principal,
                            @PathVariable Long storeId) {
        storeService.deleteStore(principal.id(), storeId);
    }
}