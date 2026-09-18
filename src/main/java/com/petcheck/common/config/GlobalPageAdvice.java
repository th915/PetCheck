package com.petcheck.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalPageAdvice {

	@Value("${kakao.map.js-key:}")
	private String kakaoJsKey;

	@ModelAttribute("kakaoJsKey")
	public String kakaoJsKey() {
		return kakaoJsKey;
	}
}
