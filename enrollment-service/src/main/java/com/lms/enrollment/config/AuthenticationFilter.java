package com.lms.enrollment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        // Đọc thông tin User ID và vai trò từ Headers do API Gateway truyền xuống
        // Read User ID and authorities from Gateway headers
        String userId = request.getHeader("X-User-Id");
        String authoritiesStr = request.getHeader("X-User-Authorities");

        log.info("AuthenticationFilter URI: {}, X-User-Id: {}, X-User-Authorities: {}", uri, userId, authoritiesStr);

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();

            if (authoritiesStr != null && !authoritiesStr.isBlank()) {
                // Phân tách các vai trò được phân chia bằng dấu phẩy
                // Split comma-separated roles into Spring authorities
                authorities = Arrays.stream(authoritiesStr.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();
            }

            // Đóng gói thông tin xác thực để lưu vào Security Context của Spring
            // Package authentication into Spring Security Context
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("Successfully authenticated user: {} with authorities: {}", userId, authorities);
        } else if (userId == null) {
            log.warn("X-User-Id is missing in request headers for URI: {}", uri);
        }
        filterChain.doFilter(request, response);
    }
}
