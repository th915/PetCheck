package com.petcheck.backend.controller.auth;

import com.petcheck.backend.common.security.MemberPrincipal;
import com.petcheck.backend.dto.auth.LoginRequest;
import com.petcheck.backend.dto.auth.MemberResponse;
import com.petcheck.backend.dto.auth.SignUpRequest;
import com.petcheck.backend.dto.auth.TokenResponse;
import com.petcheck.backend.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 토큰이 살아 있는지, 내가 누구로 인식되는지 확인. 프론트 부팅 때 호출한다 */
    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal MemberPrincipal principal) {
        return authService.findById(principal.id());
    }

    /** 회원 탈퇴. 매장·리뷰·프로필·일정·관심 장소가 함께 삭제된다 */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal MemberPrincipal principal) {
        authService.withdraw(principal.id());
    }
}