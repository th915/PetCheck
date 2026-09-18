package com.petcheck.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcheck.backend.common.exception.ErrorCode;
import com.petcheck.backend.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 로그인은 했지만 권한이 모자란 요청. 401 과 403 을 구분해야 프론트가 다르게 안내할 수 있다. */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(ErrorCode.FORBIDDEN.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(
                ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.message(), request.getRequestURI()));
    }
}