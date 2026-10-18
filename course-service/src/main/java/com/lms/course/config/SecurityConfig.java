package com.lms.course.config;

import com.lms.common.security.HeaderAuthenticationFilter;
import com.lms.common.security.InternalApiFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    @Value("${application.security.internal-key}")
    private String internalKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Tạm thời mở public cho tất cả các API để test CRUD lúc ban đầu.
                        // Sau này khi tích hợp hoàn chỉnh với API Gateway / IAM thì sẽ sửa lại
                        .requestMatchers("/api/internal/**").hasAuthority("INTERNAL")
                        .anyRequest().permitAll()
                )
                // Đăng ký filter parse header X-User-Id/X-User-Authorities từ Gateway
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new InternalApiFilter(internalKey), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
