package com.lms.finance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="vnpay")
@RefreshScope
@Data
public class VnPayConfig {
    private String tmnCode;
    private String hashSecret;
    private String payUrl;
    private String apiUrl;
    private String returnUrl;
    private String ipnUrl;
}
