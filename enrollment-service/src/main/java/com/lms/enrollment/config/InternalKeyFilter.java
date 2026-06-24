package com.lms.enrollment.config;

import com.lms.common.exception.AppException;
import com.lms.common.exception.CommonErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class InternalKeyFilter extends OncePerRequestFilter {

    @Value("${application.security.internal-key:my-secret-internal-key}")
    private String internalKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();

        // Kiểm tra xem yêu cầu có truy cập vào đường dẫn API nội bộ (/api/internal/) không
        // Check if the request targets internal API paths (/api/internal/)
        if (uri.contains("/api/internal/")) {
            String keyHeader = request.getHeader("X-Internal-Key");

            if (keyHeader == null) {
                log.error("Internal request to {} missed X-Internal-Key header", uri);
                throw new AppException(CommonErrorCode.UNAUTHORIZED, "Internal Key Header Missed");
            }

            // Kiểm tra tính hợp lệ của khóa bảo mật liên dịch vụ
            // Verify microservice communication secret key validity
            if (!keyHeader.equals(internalKey)) {
                String attackerIp = request.getRemoteAddr();
                log.error("WARNING: Unauthorized access detected to internal API!");
                log.error("Attacker IP: {}, URL requested: {}, Time: {}", attackerIp, uri, LocalDateTime.now());
                throw new AppException(CommonErrorCode.UNAUTHORIZED, "Invalid Internal Secret Key");
            }

            // Cấp quyền INTERNAL đặc biệt cho cuộc gọi dịch vụ hợp lệ
            // Grant special INTERNAL authority for validated service call
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("INTERNAL"));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "InternalService", null, authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
