# Production Deployment — fret-payment

This folder holds the production-ready Docker Compose stack for `fret-payment`.

## Stack

| Service | Image | Purpose |
|---|---|---|
| `fret-payment` | `ouail02/fret-payment:latest` | CMI Fatourati microservice (port 8082) |
| `watchtower` | `containrrr/watchtower:latest` | Auto-updates `fret-payment` when a new image is pushed to Docker Hub |

## Quick Start

```bash
# 1. Copy the template
cp .env.example .env

# 2. Fill in real values in .env
nano .env

# 3. Pull and start
docker compose up -d

# 4. Verify
docker compose ps
curl http://localhost:8082/actuator/health   # (actuator not enabled — use TCP check instead)
docker exec fret-payment sh -c "exec 3<>/dev/tcp/localhost/8082" && echo "UP"
```

## Updating

Every push to the `prod` branch in `https://github.com/OSS-NWM/fret-payment`
triggers a new CI run that builds and pushes `ouail02/fret-payment:latest` to Docker Hub.

Watchtower polls every **5 minutes**. When a new `:latest` is detected, it:
1. Pulls the new image
2. Stops the old container
3. Starts a new container with the new image
4. Removes the old image (cleanup)

To trigger an update immediately without waiting for Watchtower:
```bash
docker compose pull
docker compose up -d
```

To pause Watchtower (e.g., during maintenance):
```bash
docker compose stop watchtower
```

To resume:
```bash
docker compose start watchtower
```

To check Watchtower logs:
```bash
docker logs watchtower
```

## Auto-Update Labels

Only containers labeled `com.centurylinklabs.watchtower.enable=true` are updated.
The `fret-payment` service has this label. To add it to other services:
```yaml
services:
  my-other-service:
    labels:
      - "com.centurylinklabs.watchtower.enable=true"
```

## Health Check

`fret-payment` uses a TCP port check on 8082. If the JVM is up but the app
hasn't finished starting, the health check may still report UP before the app is
fully ready — increase `start_period` if needed.

## Logs

```bash
# App logs
docker logs fret-payment

# App logs (follow)
docker logs -f fret-payment

# Watchtower logs
docker logs watchtower
```

## Environment Variables

All secrets are read from `.env` (not committed to git). The `.env.example`
lists every variable. Key ones:

| Variable | Description |
|---|---|
| `DB_HOST` | Hostname/IP of the remote PostgreSQL server |
| `DB_PORT` | PostgreSQL port (default 5434) |
| `DB_NAME` | Database name (`fretpaymentdb`) |
| `DB_USER` | Database username |
| `DB_PASSWORD` | Database password |
| `KEYCLOAK_ISSUER` | Keycloak issuer URI |
| `FATOURATI_CALLBACK_URL` | Public-facing callback URL (used by CMI to POST back) |

## Stopping

```bash
docker compose down        # stops containers, keeps volumes
docker compose down -v      # stops and deletes volumes (WARNING: deletes app data)
```
