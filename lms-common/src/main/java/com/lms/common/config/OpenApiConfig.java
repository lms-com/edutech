package com.lms.common.config;

import com.lms.common.swagger.annotation.RequireInternalKey;
import com.lms.common.swagger.annotation.RequireJwt;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("/" + serviceName))
                .info(new Info()
                        .title("LMS Ecosystem API")
                        .version("1.0")
                        .description("Edutech LMS Ecosystem API")
                )
                .components(new Components()
                        // Them xac thuc JWT Token
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                        // Them xac thuc X-Internal-Key trong Header
                        .addSecuritySchemes("internalAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Internal-Key")
                        )
                );
    }

    // Chan bat va customize cac ham API (co @Operation) co annotation xac thuc de thuc hien xac thuc tuong ung
    @Bean
    public OperationCustomizer customizeSecurity () {
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(RequireJwt.class) ||
            handlerMethod.getBeanType().isAnnotationPresent(RequireJwt.class)) {
                operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
            }

            if (handlerMethod.hasMethodAnnotation(RequireInternalKey.class) ||
            handlerMethod.getBeanType().isAnnotationPresent(RequireInternalKey.class)) {
                operation.addSecurityItem(new SecurityRequirement().addList("internalAuth"));
            }

            return operation;
        };
    }
}