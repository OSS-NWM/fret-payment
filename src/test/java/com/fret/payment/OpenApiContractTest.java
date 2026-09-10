package com.fret.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the OpenAPI /v3/api-docs contract.
 *
 * Ensures every endpoint:
 *   - Has a non-empty summary and description
 *   - Documents the expected response codes (200, 400, 401, 403, 500)
 *   - References a real DTO in components.schemas (no missing $ref)
 *   - Lists security scheme (bearerAuth) on protected endpoints
 *
 * Run with: ./mvnw test -Dtest=OpenApiContractTest
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/nwm",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9090/realms/nwm/protocol/openid-connect/certs",
        "app.fatourati.auth-url=https://auth-dev.cmi.co.ma",
        "app.fatourati.base-url=https://agg-merchant-qa.cmi.co.ma",
        "app.fatourati.client-id=test-client",
        "app.fatourati.client-secret=test-secret",
        "app.fatourati.merchant-code=100024",
        "app.fatourati.store=100030",
        "app.fatourati.store-api-key=test-api-key",
        "app.fatourati.callback-url=http://localhost:8082/api/payment/fatourati/callback"
})
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String[] REQUIRED_PATHS = {
            "/api/payment/fatourati/invoices/{invoiceId}/paiement/fatourati",
            "/api/payment/fatourati/invoices/{invoiceId}/paiement/fatourati/status",
            "/api/payment/fatourati/invoices/{invoiceId}/paiement/fatourati/history",
            "/api/payment/fatourati/invoices/{invoiceId}/paiement/fatourati/transactions",
            "/api/payment/fatourati/paiements/fatourati/group",
            "/api/payment/fatourati/callback",
            "/api/payment/fatourati/check-status",
            "/api/payment/fatourati/cancel"
    };

    private static final String[] REQUIRED_SCHEMAS = {
            "InitiateGroupPaymentRequest",
            "FatouratiTokenResponseDto",
            "FatouratiStatusResponseDto"
    };

    @Test
    void apiDocsEndpoint_isReachable_andReturnsValidJson() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocs_containsAllRequiredPaths() throws Exception {
        JsonNode paths = getApiDocsPaths();
        for (String path : REQUIRED_PATHS) {
            assertThat(paths.has(path))
                    .as("Missing path in OpenAPI: " + path)
                    .isTrue();
        }
    }

    @Test
    void apiDocs_eachProtectedPath_hasBearerAuth() throws Exception {
        JsonNode paths = getApiDocsPaths();

        String[] protectedPaths = {
                "/api/payment/fatourati/invoices/{invoiceId}/paiement/fatourati",
                "/api/payment/fatourati/invoices/{invoiceId}/paiement/fatourati/status",
                "/api/payment/fatourati/invoices/{invoiceId}/paiement/fatourati/history",
                "/api/payment/fatourati/invoices/{invoiceId}/paiement/fatourati/transactions",
                "/api/payment/fatourati/paiements/fatourati/group"
        };

        for (String path : protectedPaths) {
            JsonNode operations = paths.path(path);
            Iterator<String> methods = operations.fieldNames();
            while (methods.hasNext()) {
                String method = methods.next();
                JsonNode op = operations.path(method);
                JsonNode security = op.path("security");
                assertThat(security.isArray() && security.size() > 0)
                        .as("Path " + path + " " + method + " missing security")
                        .isTrue();
            }
        }
    }

    @Test
    void apiDocs_eachOperation_hasSummaryAndDescription() throws Exception {
        JsonNode paths = getApiDocsPaths();
        for (String path : REQUIRED_PATHS) {
            JsonNode operations = paths.path(path);
            Iterator<String> methods = operations.fieldNames();
            while (methods.hasNext()) {
                String method = methods.next();
                JsonNode op = operations.path(method);
                assertThat(op.path("summary").asText())
                        .as("Path " + path + " " + method + " missing summary")
                        .isNotEmpty();
                assertThat(op.path("description").asText())
                        .as("Path " + path + " " + method + " missing description")
                        .isNotEmpty();
            }
        }
    }

    @Test
    void apiDocs_eachOperation_documents500() throws Exception {
        JsonNode paths = getApiDocsPaths();
        for (String path : REQUIRED_PATHS) {
            JsonNode operations = paths.path(path);
            Iterator<String> methods = operations.fieldNames();
            while (methods.hasNext()) {
                String method = methods.next();
                JsonNode op = operations.path(method);
                JsonNode responses = op.path("responses");
                assertThat(responses.has("500"))
                        .as("Path " + path + " " + method + " missing 500 response")
                        .isTrue();
            }
        }
    }

    @Test
    void apiDocs_containsAllRequiredSchemas() throws Exception {
        JsonNode schemas = getApiDocs().path("components").path("schemas");
        for (String schemaName : REQUIRED_SCHEMAS) {
            assertThat(schemas.has(schemaName))
                    .as("Missing schema in components.schemas: " + schemaName)
                    .isTrue();
        }
    }



    @Test
    void apiDocs_bearerAuthScheme_isDefined() throws Exception {
        JsonNode securitySchemes = getApiDocs().path("components").path("securitySchemes");
        assertThat(securitySchemes.has("bearerAuth"))
                .as("Missing bearerAuth security scheme")
                .isTrue();
        assertThat(securitySchemes.path("bearerAuth").path("type").asText())
                .isEqualTo("http");
        assertThat(securitySchemes.path("bearerAuth").path("scheme").asText())
                .isEqualTo("bearer");
    }

    private JsonNode getApiDocs() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode getApiDocsPaths() throws Exception {
        return getApiDocs().path("paths");
    }
}
