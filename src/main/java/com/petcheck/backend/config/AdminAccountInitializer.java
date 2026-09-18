package com.petcheck.backend.config;

import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.member.MemberRole;
import com.petcheck.backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발용 관리자 계정을 기동 시 만든다.
 * <p>
 * 관리자는 가입으로 만들 수 없게 막아뒀으므로 어딘가에서는 심어야 한다.
 * SQL 로 직접 넣으면 BCrypt 해시를 손으로 만들어야 해서 여기서 처리한다.
 * app.admin.bootstrap 이 true 일 때만 동작하고, 계정이 이미 있으면 아무것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.admin.bootstrap", havingValue = "true")
public class AdminAccountInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByEmail(email)) {
            return;
        }

        memberRepository.save(Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname("관리자")
                .role(MemberRole.ADMIN)
                .build());

        log.warn("관리자 계정을 생성했습니다 — {} / {} (배포 전 app.admin.bootstrap 을 false 로)",
                email, password);
    }
}