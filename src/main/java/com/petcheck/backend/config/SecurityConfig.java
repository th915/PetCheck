package com.petcheck.backend.config;

import com.petcheck.backend.common.security.JwtAccessDeniedHandler;
import com.petcheck.backend.common.security.JwtAuthenticationEntryPoint;
import com.petcheck.backend.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 경로별 접근 규칙.
 * <p>
 * 규칙은 위에서 아래로 먼저 맞는 것이 이긴다. 넓은 규칙을 위에 두면
 * 아래의 좁은 규칙이 영영 안 걸리므로, 공개 경로를 먼저 나열하고
 * 마지막에 anyRequest().authenticated() 로 닫는다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)   // 401
                        .accessDeniedHandler(accessDeniedHandler))            // 403

                .authorizeHttpRequests(auth -> auth
                        // --- 공개 ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/places/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // --- 공개 ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/places/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // 리뷰 목록만 공개. /me, 작성, 수정, 삭제는 고객 전용이다.
                        // "/api/reviews/**" 로 열어두면 /me 까지 공개돼 principal 이 null 로 들어온다.
                        .requestMatchers(HttpMethod.GET, "/api/reviews").permitAll()
                        .requestMatchers("/api/reviews/**").hasRole("CUSTOMER")

                        // --- 역할 기반 ---
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/store/**").hasRole("STORE")

                        // --- 역할 기반 ---
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/store/**").hasRole("STORE")

                        // --- 나머지는 로그인 필요 ---
                        .anyRequest().authenticated()
                )

                // 스프링 기본 인증 필터보다 앞에 둬야 토큰을 먼저 읽는다
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}