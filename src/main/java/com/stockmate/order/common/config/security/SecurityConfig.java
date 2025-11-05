package com.stockmate.order.common.config.security;

import com.stockmate.order.common.config.filter.JwtHeaderFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtHeaderFilter jwtHeaderFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 비활성화 (Stateless API)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션을 사용하지 않음 (Stateless API)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Form 로그인 및 HTTP Basic 인증 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // 우리가 만든 JwtHeaderFilter를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtHeaderFilter, UsernamePasswordAuthenticationFilter.class)

                // 모든 요청은 인증되어야 함 (필터에서 인증 객체를 만들어주므로)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/api-doc", "/health", "/v3/api-docs/**",
                                "/swagger-resources/**","/swagger-ui/**",
                                "/h2-console/**", "/v3/api-docs"
                        ).permitAll() // 스웨거, H2, healthCheck 허가
                        .requestMatchers(
                                "/api/v1/order/health-check", "/api/v1/order/navigation/parts", "/api/v1/order/deposit-detail"
                        ).permitAll()
                        .requestMatchers(
                                "/ws/**"
                        ).permitAll() // WebSocket 경로 허용
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}