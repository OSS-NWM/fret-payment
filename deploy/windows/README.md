# Fret-Payment — Windows Deployment

Run fret-payment directly on Windows (no Docker required).

## Prerequisites

- **Java 17 JDK** installed
- `java` command available in `PATH`
- Verify: open `cmd` and run `java -version`

## Files

| File | Purpose |
|---|---|
| `fret-payment-0.0.1-SNAPSHOT.jar` | Application JAR |
| `setenv.bat` | Environment variables — **edit this** with your values |
| `start.bat` | Launch script — run this to start the service |
| `README.md` | This file |

## Setup

**1. Edit `setenv.bat`**
Open `setenv.bat` in Notepad and update values if needed:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — your PostgreSQL server
- `KEYCLOAK_ISSUER`, `KEYCLOAK_JWKS` — your Keycloak instance
- `SERVER_PORT` — port for the app (default `8000`)
- `FATOURATI_CALLBACK_URL` — public URL CMI will call back

**2. Start the service**
```
Double-click start.bat
```
Or from `cmd`:
```
cd path\to\this\folder
start.bat
```

The service will start on **`http://localhost:8000`**.

## Verify

```powershell
curl http://localhost:8000/api/payment/fatourati/check-status?token_ref=test
```

Or open in browser: `http://localhost:8000/actuator/health`

## Stop

Press `Ctrl+C` in the terminal window running the service.

## Troubleshooting

**`JAVA_HOME` not set or wrong version**
```cmd
java -version
```
Must show Java 17+. If not, install JDK 17 from https://adoptium.net

**DB connection refused**
Verify `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` in `setenv.bat`
and that the PostgreSQL server is reachable from this machine.

**Keycloak auth failures**
Verify `KEYCLOAK_ISSUER` and `KEYCLOAK_JWKS` match your Keycloak realm config.
