package com.petcheck.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MapPageController {

	@GetMapping("/")
	public String mapPage() {
		return "place/map";
	}
}
