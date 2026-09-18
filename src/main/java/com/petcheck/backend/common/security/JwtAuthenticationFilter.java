package com.petcheck.backend.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authorization 헤더의 토큰을 읽어 SecurityContext 에 인증을 심는다.
 * <p>
 * 토큰이 없거나 잘못됐어도 여기서 401 을 만들지 않는다.
 * 그 경로가 인증이 필요한지는 시큐리티 설정이 판단할 일이고,
 * 필터가 미리 막으면 공개 API 까지 토큰을 요구하게 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                MemberPrincipal principal = tokenProvider.parse(token);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.authorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException | IllegalArgumentException e) {
                // 만료·위조·형식 오류. 인증 없이 통과시키고 뒤에서 401 이 나가게 둔다.
                SecurityContextHolder.clearContext();
                log.debug("JWT 검증 실패 — {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);

        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }
        return header.substring(PREFIX.length()).trim();
    }
}