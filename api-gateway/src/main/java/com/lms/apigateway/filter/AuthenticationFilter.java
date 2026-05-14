package com.lms.apigateway.filter;

import com.lms.apigateway.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtils jwtUtils;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthenticationFilter (JwtUtils jwtUtils, ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            log.info(">>>> ĐÃ CHẠM VÀO FILTER RỒI NÈ!"); // Thêm dòng này
            ServerHttpRequest request = exchange.getRequest();

            // Kiem tra request co Authentication trong Header va Authentication co thuoc dang Bearer khong
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.error("❌ Request {} missed", request.getHeaders());
                return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
            }
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                jwtUtils.validateToken(token);
                // Lay thong tin tu token
                String userId = jwtUtils.getUserId(token);
                List<String> authorities = jwtUtils.getAuthorities(token);
                String deviceFingerPrint = jwtUtils.getDeviceFingerPrint(token);
                String redisKey = "user:" + userId + ":device";

                String authoritiesStr = String.join(",", authorities);

                return redisTemplate.opsForZSet().score(redisKey, deviceFingerPrint)
                        .switchIfEmpty(Mono.error(new RuntimeException("DEVICE_NOT_FOUND")))
                        .flatMap(score -> {
                            log.info("Device {} of user {} is valid", deviceFingerPrint, userId);
                            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .header("X-User-Id", userId)
                                    .header("X-User-Authorities", authoritiesStr)
                                    .build();
                            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                            return chain.filter(mutatedExchange);
                        })
                        .onErrorResume(e -> {
                            if ("DEVICE_NOT_FOUND".equals(e.getMessage())) {
                                log.warn("Device {} of user {} was kicked or not found", deviceFingerPrint, userId);
                                return onError(exchange, "Device has been logged out due to multi-device limit", HttpStatus.UNAUTHORIZED);
                            }
                            // Các lỗi khác (nếu có)
                            return onError(exchange, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
                        });
            } catch (Exception e) {
                log.error("Invalid or Expired Token: {}", e.getMessage());
                return onError(exchange, "Invalid or Expired Token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError (ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"code\": %d, \"message\": \"%s\", \"data\": null}", status.value(), message);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}