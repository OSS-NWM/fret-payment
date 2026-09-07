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

## API Documentation (Swagger/OpenAPI)

Interactive API docs are available once the service is running:

```bash
# Local
http://localhost:8082/swagger-ui.html

# Raw OpenAPI JSON
http://localhost:8082/v3/api-docs
```

**JWT roles required** (for protected endpoints): `OPERATEUR_COMMUNITY`, `AGENT_FACTURATION_NWM`, `RESPONSABLE_FACTURATION_NWM`

**Public endpoints** (signature-verified, no JWT): `/callback`, `/cancel`, `/check-status`

**Base URL for all endpoints**: `/api/payment/fatourati`

### JWT format

```json
Authorization: Bearer <keycloak-jwt>
```

Use the Keycloak realm `fret-management-client` to obtain a token via `client_credentials` or `password` grant.

## Callback Configuration

CMI calls back to three URLs that are embedded in every token creation request. These **must** point to the publicly accessible address of the `fret-payment` backend — not a proxy.

| Property | Env Variable | Purpose |
|----------|-------------|---------|
| `callbackUrl` | `FATOURATI_CALLBACK_URL` | CMI calls this on payment confirmation |
| `cancelUrl` | `FATOURATI_CANCEL_URL` | CMI calls this on payment cancellation |
| `checkStatusUrl` | `FATOURATI_CHECK_STATUS_URL` | CMI polls this for status updates |

All three are sent to CMI in the `generateToken` request inside `orderLinks`:

```json
{
  "orderLinks": {
    "callbackURL": "http://51.170.134.229:8000/api/payment/fatourati/callback",
    "cancelURL": "http://51.170.134.229:8000/api/payment/fatourati/cancel",
    "checkStatusURL": "http://51.170.134.229:8000/api/payment/fatourati/check-status"
  }
}
```

**Windows deploy** (`deploy/windows/setenv.bat`): all three URLs are set explicitly (not derived by string replacement). CMI must be able to reach these URLs from the internet.
