# Fret Payment Service

CMI Fatourati payment microservice for Nador West Med. Handles token generation, payment confirmation callbacks, and status queries via the CMI merchant API.

**Tech Stack**: Java 17, Spring Boot 3.2.5, Maven, PostgreSQL, Keycloak OAuth2, Lombok

## Build & Run

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test
```

## Database

PostgreSQL on port **5434**, database `fretpaymentdb`. Schema auto-managed via JPA `ddl-auto=update`.

```bash
# Start the DB container
docker-compose up -d

# Seed (no reference data needed)
psql -h localhost -p 5434 -U postgres -d fretpaymentdb -f insert-fretpayment-ref-data.sql

# Reset data
psql -h localhost -p 5434 -U postgres -d fretpaymentdb -f reset-fretpayment-data.sql
```

## Configuration

Keycloak issuer: `http://localhost:9090/realms/nwm` (override via `KEYCLOAK_ISSUER` env var).

CMI sandbox credentials are defaults in `application.properties` — rotate before production.

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/payment/fatourati/callback` | Public (signature) | CMI payment confirmation callback |
| POST | `/api/payment/fatourati/cancel` | Public (signature) | CMI cancel callback |
| GET | `/api/payment/fatourati/check-status` | Public | Token status polling by CMI |
| POST | `/api/payment/fatourati/mouvement/{id}/paiement/fatourati` | JWT | Initiate payment for a mouvement |
| GET | `/api/payment/fatourati/mouvement/{id}/paiement/fatourati/status` | JWT | Get payment status |
| DELETE | `/api/payment/fatourati/mouvement/{id}/paiement/fatourati` | JWT | Cancel payment |
