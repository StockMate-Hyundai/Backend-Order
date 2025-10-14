package com.stockmate.order.common.config.filter;

import com.stockmate.order.common.config.security.Role;
import com.stockmate.order.common.config.security.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHeaderFilter extends OncePerRequestFilter {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String MEMBER_ROLE_HEADER = "X-Member-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 게이트웨이에서 전달된 헤더 정보 추출
        String memberIdStr = request.getHeader(MEMBER_ID_HEADER);
        String roleStr = request.getHeader(MEMBER_ROLE_HEADER);

        // 필수 헤더가 없으면 인증 객체를 생성하지 않고 다음 필터로 진행
        if (!StringUtils.hasText(memberIdStr) || !StringUtils.hasText(roleStr)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 헤더 정보를 파싱하여 SecurityUser 객체 생성
            Long memberId = Long.parseLong(memberIdStr);
            Role role = Role.valueOf(roleStr.toUpperCase());

            SecurityUser securityUser = SecurityUser.builder()
                    .memberId(memberId)
                    .role(role)
                    .build();

            // Spring Security가 사용할 인증 토큰 생성
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    securityUser,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority(role.getKey()))
            );

            // SecurityContextHolder에 인증 정보 등록
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("인증 성공: SecurityUser(Member ID '{}', Role '{}')가 Security Context에 저장되었습니다.", memberId, role);

        } catch (NumberFormatException e) {
            log.error("잘못된 Member ID 형식입니다: {}", memberIdStr, e);
        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 Role 형식입니다: {}", roleStr, e);
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}