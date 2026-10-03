package com.lms.order.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

//@Configuration
public class OpenApiConfig {

    //@Bean
    public OpenAPI customOrderServiceOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:8080");
        server.setDescription("Order Service Apis Documentation");

        Info info = new Info();
        info.setTitle("Order Service Apis Documentation");
        info.setDescription("Order Service Apis Documentation");
        info.setVersion("1.0");
        info.setLicense(new License().name("Apache 2.0"));
        return new OpenAPI().info(info).servers(List.of(server));
    }


}
