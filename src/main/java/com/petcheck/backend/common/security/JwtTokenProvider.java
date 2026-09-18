package com.petcheck.backend.common.security;

import com.petcheck.backend.common.properties.JwtProperties;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.member.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * 토큰 발급과 검증만 한다. 인증 여부 판단은 필터가, 권한 판단은 시큐리티 설정이 한다.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final Duration validity;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.validity = properties.accessTokenValidity();
    }

    public String createToken(Member member) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim("email", member.getEmail())
                .claim("role", member.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /**
     * 서명과 만료를 검증하고 본문을 꺼낸다.
     * 검증에 실패하면 jjwt 가 JwtException 을 던진다 — 여기서 잡지 않고 필터로 올린다.
     */
    public MemberPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new MemberPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                MemberRole.valueOf(claims.get("role", String.class)));
    }

    public Duration validity() {
        return validity;
    }
}