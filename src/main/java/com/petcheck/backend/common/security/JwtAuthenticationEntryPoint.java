package com.petcheck.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcheck.backend.common.exception.ErrorCode;
import com.petcheck.backend.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증되지 않은 요청이 보호된 경로에 왔을 때.
 * <p>
 * 이게 없으면 스프링 기본 401 이 HTML 로 나가고, JSON 을 기대한 프론트가 파싱에서 죽는다.
 * GlobalExceptionHandler 는 컨트롤러 밖에서 일어난 일을 못 잡으므로 따로 필요하다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(ErrorCode.UNAUTHORIZED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(
                ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.message(), request.getRequestURI()));
    }
}