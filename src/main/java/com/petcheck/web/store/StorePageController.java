package com.petcheck.web.store;

import com.petcheck.backend.common.security.MemberPrincipal;
import com.petcheck.backend.entity.member.MemberRole;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 사장님 대시보드 화면 껍데기. 실제 등록·수정·삭제는 JS 가
 * /api/store/** 를 직접 호출한다 (쿠키에 담긴 JWT 로 인증됨).
 */
@Controller
public class StorePageController {

	@GetMapping("/store/dashboard")
	public String dashboard(@AuthenticationPrincipal MemberPrincipal principal) {
		String guard = requireStoreRole(principal);
		if (guard != null) {
			return guard;
		}
		return "store/dashboard";
	}

	@GetMapping("/store/register")
	public String register(@AuthenticationPrincipal MemberPrincipal principal) {
		String guard = requireStoreRole(principal);
		if (guard != null) {
			return guard;
		}
		return "store/register";
	}

	@GetMapping("/store/{id}/checklist")
	public String checklist(@PathVariable Long id, @AuthenticationPrincipal MemberPrincipal principal,
			Model model) {
		String guard = requireStoreRole(principal);
		if (guard != null) {
			return guard;
		}
		model.addAttribute("storeId", id);
		return "store/checklist";
	}

	private String requireStoreRole(MemberPrincipal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		if (principal.role() != MemberRole.STORE) {
			return "redirect:/";
		}
		return null;
	}
}
