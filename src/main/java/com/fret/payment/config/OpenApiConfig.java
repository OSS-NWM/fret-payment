package com.fret.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fret Payment — CMI Fatourati API")
                        .version("1.0.0")
                        .description("""
                                Microservice for managing CMI Fatourati payment tokens, callbacks, and status queries.

                                ## Architecture
                                - Port 8082, Spring Boot 3.2.5, Java 17
                                - JWT authentication via Keycloak (`fret-management-client`)
                                - CMI Fatourati sandbox: `auth-dev.cmi.co.ma` + `agg-merchant-qa.cmi.co.ma`

                                ## Security
                                - **Public (signature-verified):** `/callback`, `/check-status`, `/cancel`
                                - **JWT-protected:** all other endpoints require a valid Keycloak JWT with one of:
                                  `OPERATEUR_COMMUNITY`, `AGENT_FACTURATION_NWM`, `RESPONSABLE_FACTURATION_NWM`

                                ## Data flows
                                1. Frontend → `POST /mouvement/{id}/paiement/fatourati` → CMI token generated
                                2. User pays via CMI QR/link
                                3. CMI → `POST /callback` → payment confirmed
                                4. `POST /internal/fatourati/payment-confirmed` → fret-management updated
                                """)
                        .contact(new Contact()
                                .name("Nador West Med — IT Team")
                                .email("it@nadorwestmed.ma"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://nadorwestmed.ma")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Local development"),
                        new Server()
                                .url("http://51.170.134.229:8000")
                                .description("Test server"),
                        new Server()
                                .url("https://api.nadorwestmed.ma")
                                .description("Production")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Keycloak JWT from `fret-management-client` realm. "
                                        + "Roles: OPERATEUR_COMMUNITY, AGENT_FACTURATION_NWM, RESPONSABLE_FACTURATION_NWM")));
    }
}
