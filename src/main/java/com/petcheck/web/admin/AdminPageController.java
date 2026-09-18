package com.petcheck.web.admin;

import com.petcheck.backend.common.security.MemberPrincipal;
import com.petcheck.backend.entity.member.MemberRole;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

	@GetMapping("/admin/stores")
	public String storeApproval(@AuthenticationPrincipal MemberPrincipal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		if (principal.role() != MemberRole.ADMIN) {
			return "redirect:/";
		}
		return "admin/stores";
	}
}
