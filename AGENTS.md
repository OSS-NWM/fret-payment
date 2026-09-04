# OSS-NWM — fret-payment

This file provides guidance when working with the `fret-payment` codebase.

## Project Overview

CMI Fatourati payment microservice. Hexagonal architecture (clean architecture) for managing Fatourati payment tokens, callbacks, and status queries via the CMI merchant API. Uses JWT security (Keycloak), JPA persistence (PostgreSQL), and Lombok.

**Tech Stack**: Java 17, Spring Boot 3.2.5, Maven, PostgreSQL 5434, Keycloak OAuth2, Lombok

## Build & Run

```bash
./mvnw clean install
./mvnw spring-boot:run   # port 8082
./mvnw test
```

## Architecture

Hexagonal layering, dependency rule `adapter → application → domain`:
- `domain/` — pure Java, NO framework annotations.
- `application/service/` — use-case implementations, `@Service` here only.
- `adapter/in/rest/` — controllers (`@RestController`).
- `adapter/out/` — CMI client adapter, JPA entities/repositories.

## Database

- `fretpaymentdb` on port 5434.
- `ddl-auto=update` — schema auto-managed. No Flyway config despite `V*` naming in `sql/payment/`.
- Two tables: `fatourati_token`, `fatourati_callback_log`.

## Security

- Callback/cancel/check-status endpoints are **public** (signature-verified, no JWT).
- All other endpoints require a valid JWT from Keycloak with `fret-management-client` realm.
- SecurityConfig permits only the 3 CMI webhook endpoints; everything else requires authentication.

## CMI Integration

- Sandbox: `auth-dev.cmi.co.ma` + `agg-merchant-qa.cmi.co.ma`
- `generateToken()` calls `getAccessToken()` **twice** (once before POST, once before GET in `enrichTokenFromGetToken`).
- OAuth token cached in `CmiAccessTokenCacheService`.
- All Novu notifications fire-and-forget — never throw.

## Cross-service calls

- `InvoiceInfoHttpAdapter` calls `http://localhost:8081/api/invoices/mouvement/{id}` to fetch invoice amount before token generation. If fret-management is unavailable, a default amount of 100.00 MAD is used.
