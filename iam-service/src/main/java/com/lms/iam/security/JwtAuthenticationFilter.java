package com.lms.iam.security;

import com.lms.common.exception.AppException;
import com.lms.iam.exception.IamErrorCode;
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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            String email = jwtService.userName(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Lay authorities String tu token va chuyen lai thanh SimpleGrantedAuthority
                List<SimpleGrantedAuthority> authorities = jwtService.getAuthorities(token).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                // Thuc hien gan authorities vao Authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                // Gan Authentication vao Security context holder la xong
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }catch (Exception e){
            log.error("Chi tiết lỗi JWT tại Service: ", e);
            // !!! Tam thoi giu de develop. Se xoa sau nay de Security Config response 403
            throw new AppException(IamErrorCode.JWT_TOKEN_INVALID, "JwtAuthenticationFilter at IAM ERROR: Jwt token invalid");
        }
        filterChain.doFilter(request, response);
    }
}