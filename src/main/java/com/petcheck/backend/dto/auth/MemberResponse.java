package com.petcheck.backend.dto.auth;

import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.member.MemberRole;

/** 비밀번호 해시는 어떤 응답에도 나가지 않는다. 그래서 엔티티를 직접 반환하지 않는다. */
public record MemberResponse(Long id, String email, String nickname, MemberRole role) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(), member.getEmail(), member.getNickname(), member.getRole());
    }
}