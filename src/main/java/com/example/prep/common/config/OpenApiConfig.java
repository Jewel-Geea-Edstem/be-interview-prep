package com.example.prep.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  public static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI openApi() {
    SecurityScheme bearer =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Paste the accessToken returned by POST /api/v1/auth/login");
    return new OpenAPI()
        .info(
            new Info()
                .title("be-interview-prep API")
                .version("v1")
                .description(
                    "Tasks, URL shortener, authentication, product catalog and orders. "
                        + "Log in, then use Authorize with the access token."))
        .components(new Components().addSecuritySchemes(BEARER_AUTH, bearer))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }
}
