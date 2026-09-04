package com.fret.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
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
class FretPaymentApplicationTests {

    @Test
    void contextLoads() {
    }
}
