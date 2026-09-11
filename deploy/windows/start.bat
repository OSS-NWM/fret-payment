@echo off
chcp 65001 >nul
title fret-payment

echo ================================================
echo  Fret-Payment  (port 8000)
echo ================================================

set JAR_FILE=fret-payment-0.0.1-SNAPSHOT.jar

if not exist "%~dp0%JAR_FILE%" (
    echo [ERROR] JAR not found: %~dp0%JAR_FILE%
    echo Copy the JAR file into this folder first.
    pause
    exit /b 1
)

if not exist "%~dp0setenv.bat" (
    echo [ERROR] setenv.bat not found. Copy setenv.template.bat to setenv.bat
    echo and fill in your environment values first.
    pause
    exit /b 1
)

echo Loading environment ...
call "%~dp0setenv.bat"

echo.
echo  Server port : %SERVER_PORT%
echo  DB host     : %DB_HOST%:%DB_PORT%/%DB_NAME%
echo  Keycloak    : %KEYCLOAK_ISSUER%
echo.

echo Starting fret-payment ...
echo (Press Ctrl+C to stop)
echo.

java -jar "%~dp0%JAR_FILE%" ^
    --server.address=0.0.0.0 ^
    --server.port=%SERVER_PORT% ^
    --spring.datasource.url=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME% ^
    --spring.datasource.username=%DB_USER% ^
    --spring.datasource.password=%DB_PASSWORD% ^
    --spring.security.oauth2.resourceserver.jwt.issuer-uri=%KEYCLOAK_ISSUER% ^
    --spring.security.oauth2.resourceserver.jwt.jwk-set-uri=%KEYCLOAK_JWKS% ^
    --app.fatourati.auth-url=%FATOURATI_AUTH_URL% ^
    --app.fatourati.auth-realm=%FATOURATI_AUTH_REALM% ^
    --app.fatourati.base-url=%FATOURATI_BASE_URL% ^
    --app.fatourati.api-version=%FATOURATI_API_VERSION% ^
    --app.fatourati.client-id=%FATOURATI_CLIENT_ID% ^
    --app.fatourati.client-secret=%FATOURATI_CLIENT_SECRET% ^
    --app.fatourati.merchant-code=%FATOURATI_MERCHANT_CODE% ^
    --app.fatourati.store=%FATOURATI_STORE% ^
    --app.fatourati.store-api-key=%FATOURATI_STORE_API_KEY% ^
    --app.fatourati.signature-algorithm=%FATOURATI_SIGNATURE_ALGORITHM% ^
    --app.fatourati.cashier-id=%FATOURATI_CASHIER_ID% ^
    --app.fatourati.callback-url=%FATOURATI_CALLBACK_URL% ^
    --app.fatourati.callback-secret=%FATOURATI_CALLBACK_SECRET% ^
    --app.fatourati.client-name=%FATOURATI_CLIENT_NAME% ^
    --app.fatourati.client-email=%FATOURATI_CLIENT_EMAIL% ^
    --app.fatourati.client-phone=%FATOURATI_CLIENT_PHONE% ^
    --app.fatourati.token-ttl-minutes=%FATOURATI_TOKEN_TTL%
