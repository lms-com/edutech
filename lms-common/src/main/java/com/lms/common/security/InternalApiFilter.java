package com.lms.common.security;

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

@Slf4j
public class InternalApiFilter  extends OncePerRequestFilter {

    private String internalKey;

    public InternalApiFilter(String internalKey) {
        this.internalKey = internalKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String url = request.getRequestURL().toString();
        // Kiem tra request ko phai noi bo thi khong xu li tiep
        if (!url.contains("/api/internal/v1")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Neu cua noi bo thi lay secret key de doi chieu
        String authHeader = request.getHeader("X-Internal-Key");
        if (authHeader == null ) {
            // Header ko co thi nem loi 401
            log.error("Request {} missed header Internal Key", request.getRequestURL());
            throw new AppException(CommonErrorCode.UNAUTHORIZED, "Internal Key Header Missed");
        }
        String secretKey = request.getHeader("X-Internal-Key");
        if (!authHeader.equals(internalKey)) {
            // Key sai thi nem loi 401
            String attackerIp = request.getRemoteAddr();
            log.error("CẢNH BÁO: Phát hiện truy cập trái phép vào API Internal!");
            log.error("Địa chỉ IP: {}", attackerIp);
            log.error("URL yêu cầu: {}", request.getRequestURI());
            log.error("Thời điểm: {}", LocalDateTime.now());
            throw new AppException(CommonErrorCode.UNAUTHORIZED, "Invalid Internal Secret Key");
        }

        // Kiem tra hoan thanh tien hanh tao authentication cho spring security
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("INTERNAL"));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "InternalService",
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
