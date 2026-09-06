@echo off
rem ============================================================
rem  Fret-Payment — Environment Variables
rem  Edit the values below to match your environment,
rem  then run start.bat
rem ============================================================

set TZ=Africa/Casablanca

rem ─── Database ───
set DB_HOST=10.0.1.77
set DB_PORT=5432
set DB_NAME=fretpaymentdb
set DB_USER=postgres
set DB_PASSWORD=T@ngerMed2024

rem ─── Keycloak ───
set KEYCLOAK_ISSUER=http://84.8.223.202:8080/realms/Fret-Management
set KEYCLOAK_JWKS=http://84.8.223.202:8080/realms/Fret-Management/protocol/openid-connect/certs

rem ─── Server ───
set SERVER_PORT=8000

rem ─── CMI Fatourati ───
set FATOURATI_AUTH_URL=https://auth-dev.cmi.co.ma
set FATOURATI_AUTH_REALM=pay-gate-ext-qa
set FATOURATI_BASE_URL=https://agg-merchant-qa.cmi.co.ma
set FATOURATI_API_VERSION=v1
set FATOURATI_CLIENT_ID=Client_NadorWestmed
set FATOURATI_CLIENT_SECRET=M2dC0VFRYughLe8z9TCEbIdFNhKCKDLW
set FATOURATI_MERCHANT_CODE=100024
set FATOURATI_STORE=100030
set FATOURATI_STORE_API_KEY=6HQ7F3HTPNF12IDEQVQDHU0YJ6UAS9P5
set FATOURATI_SIGNATURE_ALGORITHM=HMAC-SHA256
set FATOURATI_CASHIER_ID=1
set FATOURATI_CALLBACK_URL=http://51.170.134.229:8000/api/payment/fatourati/callback
set FATOURATI_CALLBACK_SECRET=w23M2dC0VFRYughLe8z9TCEbIdFNhKCKDLW
set FATOURATI_CLIENT_NAME=Client Nador West Med
set FATOURATI_CLIENT_EMAIL=contact@nadorwestmed.ma
set FATOURATI_CLIENT_PHONE=+212600000000
set FATOURATI_TOKEN_TTL=60
