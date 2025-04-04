package kr.cocoh.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${spring.application.name:광고 관리 시스템}")
    private String applicationName;

    @Value("${springdoc.server.url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        // API 정보 설정
        Info info = new Info()
                .title(applicationName + " API")
                .version("1.0.0")
                .description("광고 관리 시스템 API 문서")
                .termsOfService("https://cocoh.kr/terms")
                .contact(new Contact()
                        .name("CO+COH")
                        .url("https://cocoh.kr")
                        .email("support@cocoh.kr"))
                .license(new License()
                        .name("Private License")
                        .url("https://cocoh.kr/license"));

        // 보안 스키마 설정
        SecurityScheme securityScheme = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        // 서버 정보 설정
        Server server = new Server()
                .url(serverUrl)
                .description("Server URL");

        // OpenAPI 설정
        return new OpenAPI()
                .info(info)
                .servers(List.of(server))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth"));
    }
}