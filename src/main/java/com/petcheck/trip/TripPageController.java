package com.petcheck.trip;

import com.petcheck.backend.common.security.MemberPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TripPageController {

	@GetMapping("/trips/new")
	public String createPage(@AuthenticationPrincipal MemberPrincipal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		return "trip/create";
	}

	@GetMapping("/trips")
	public String listPage(@AuthenticationPrincipal MemberPrincipal principal) {
		if (principal == null) {
			return "redirect:/login";
		}
		return "trip/list";
	}

	@GetMapping("/trips/{id}")
	public String detailPage(@PathVariable Long id, @AuthenticationPrincipal MemberPrincipal principal,
			Model model) {
		if (principal == null) {
			return "redirect:/login";
		}
		model.addAttribute("tripId", id);
		return "trip/detail";
	}
}
