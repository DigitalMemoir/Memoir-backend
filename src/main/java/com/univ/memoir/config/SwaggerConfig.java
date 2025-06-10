package com.univ.memoir.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT";

        return new OpenAPI()
                .info(new Info()
                        .title("Collec API Document")
                        .version("v0.0.1")
                        .description("collec API 문서입니다."))
                .addSecurityItem(new SecurityRequirement().addList(jwtSchemeName))
                .components(new Components()
                        .addSecuritySchemes(jwtSchemeName,
                                new SecurityScheme()
                                        .name("Authorization")      // header 이름
                                        .type(SecurityScheme.Type.APIKEY) // bearer 대신 apiKey
                                        .in(SecurityScheme.In.HEADER)     // 헤더에 설정
                        ));
    }
}
