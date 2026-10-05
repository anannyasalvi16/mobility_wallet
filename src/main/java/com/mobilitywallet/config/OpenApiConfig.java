package com.mobilitywallet.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger documentation configuration.
 * Exposes interactive API documentation at /swagger-ui.html and spec at /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mobility Wallet API")
                        .version("1.0")
                        .description("Enterprise REST API for closed-loop prepaid transit allowance vouchers, " +
                                "wallet transactions, high-concurrency seat bookings, and taxi ride QR redemptions.")
                        .contact(new Contact()
                                .name("Mobility Wallet Engineering")
                                .email("dev@mobility.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("HTTP Basic Authentication for /api/** endpoints. " +
                                                "Example: admin@mobility.com / admin123 (Admin) or user@mobility.com / user123 (Commuter).")));
    }
}
