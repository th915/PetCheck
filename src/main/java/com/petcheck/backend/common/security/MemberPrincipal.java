package com.petcheck.backend.common.security;

import com.petcheck.backend.entity.member.MemberRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * 인증된 사용자. 컨트롤러에서 @AuthenticationPrincipal 로 받는다.
 * <p>
 * Member 엔티티를 그대로 쓰지 않는 이유: 요청마다 DB 를 한 번 더 읽게 되고,
 * 영속성 컨텍스트 밖의 엔티티를 들고 다니면 지연 로딩에서 사고가 난다.
 * 토큰 안에 든 최소 정보만 담는다.
 */
public record MemberPrincipal(Long id, String email, MemberRole role) {

    /**
     * hasRole("ADMIN") 은 "ROLE_ADMIN" 권한을 찾는다.
     * 접두사를 여기서 붙이지 않으면 시큐리티 설정이 조용히 전부 거부한다.
     */
    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}