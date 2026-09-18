package com.petcheck.web.auth;

import com.petcheck.backend.common.exception.BusinessException;
import com.petcheck.backend.dto.auth.LoginRequest;
import com.petcheck.backend.dto.auth.SignUpRequest;
import com.petcheck.backend.dto.auth.TokenResponse;
import com.petcheck.backend.entity.member.MemberRole;
import com.petcheck.backend.service.auth.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 친구가 만든 JWT REST API(/api/auth/**)를 그대로 쓰되, 브라우저 화면 쪽에서는
 * 발급받은 토큰을 HttpOnly 쿠키(ACCESS_TOKEN)로 심어준다.
 * 이후 페이지의 JS fetch() 는 같은 오리진이라 쿠키를 자동으로 실어 보내고,
 * JwtAuthenticationFilter 가 헤더 다음으로 이 쿠키를 읽는다.
 */
@Controller
@RequiredArgsConstructor
public class AuthPageController {

	private static final String COOKIE_NAME = "ACCESS_TOKEN";

	private final AuthService authService;

	@GetMapping("/login")
	public String loginPage(@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "logout", required = false) String logout,
			@RequestParam(value = "signup", required = false) String signup,
			Model model) {
		if (error != null) {
			model.addAttribute("errorMessage", "이메일 또는 비밀번호가 올바르지 않습니다.");
		}
		if (logout != null) {
			model.addAttribute("logoutMessage", "로그아웃되었습니다.");
		}
		if (signup != null) {
			model.addAttribute("signupMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
		}
		return "auth/login";
	}

	@PostMapping("/login")
	public String login(@RequestParam String email, @RequestParam String password,
			HttpServletResponse response, Model model) {
		try {
			TokenResponse token = authService.login(new LoginRequest(email, password));
			setTokenCookie(response, token);
			return "redirect:/";
		} catch (BusinessException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return "auth/login";
		}
	}

	@GetMapping("/signup")
	public String signupPage(Model model) {
		return "auth/signup";
	}

	@PostMapping("/signup")
	public String signup(@RequestParam String email, @RequestParam String password,
			@RequestParam String nickname, @RequestParam MemberRole role, Model model) {
		try {
			authService.signUp(new SignUpRequest(email, password, nickname, role));
			return "redirect:/login?signup";
		} catch (BusinessException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return "auth/signup";
		}
	}

	@PostMapping("/logout")
	public String logout(HttpServletResponse response) {
		ResponseCookie clear = ResponseCookie.from(COOKIE_NAME, "")
				.httpOnly(true)
				.path("/")
				.maxAge(0)
				.sameSite("Lax")
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());
		return "redirect:/login?logout";
	}

	private void setTokenCookie(HttpServletResponse response, TokenResponse token) {
		ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token.accessToken())
				.httpOnly(true)
				.path("/")
				.maxAge(token.expiresIn())
				.sameSite("Lax")
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
