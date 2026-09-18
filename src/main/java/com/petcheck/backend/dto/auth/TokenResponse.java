package com.petcheck.backend.dto.auth;

import com.petcheck.backend.entity.member.Member;

import java.time.Duration;

/**
 * @param tokenType Authorization 헤더에 붙일 접두사. 프론트가 문자열을 하드코딩하지 않게 내려준다
 * @param expiresIn 만료까지 남은 초. 프론트가 재로그인 시점을 판단한다
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        MemberResponse member
) {

    public static TokenResponse of(String accessToken, Duration validity, Member member) {
        return new TokenResponse(accessToken, "Bearer", validity.toSeconds(),
                MemberResponse.from(member));
    }
}