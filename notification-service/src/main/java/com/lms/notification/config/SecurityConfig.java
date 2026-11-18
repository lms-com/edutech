package com.lms.notification.config;

import com.lms.common.security.InternalApiFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationFilter authenticationFilter;

    @Value("${application.security.internal-key}")
    private String internalKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Bảo vệ các API nội bộ giữa các microservice bằng X-Internal-Key
                        .requestMatchers("/api/internal/**").hasAuthority("INTERNAL")
                        // Cho phép truy cập không cần auth đối với endpoint subscribe SSE, verify certificate hoặc swagger
                        .requestMatchers("/api/v1/notifications/subscribe").permitAll()
                        .requestMatchers("/api/v1/certificates/verify/**").permitAll()
                        .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        // Các request còn lại bắt buộc đã đăng nhập
                        .anyRequest().authenticated()
                )
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new InternalApiFilter(internalKey), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
