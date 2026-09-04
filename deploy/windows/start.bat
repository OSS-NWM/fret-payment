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
    --server.port=%SERVER_PORT% ^
    -Dspring.datasource.url=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME% ^
    -Dspring.datasource.username=%DB_USER% ^
    -Dspring.datasource.password=%DB_PASSWORD% ^
    -Dspring.security.oauth2.resourceserver.jwt.issuer-uri=%KEYCLOAK_ISSUER% ^
    -Dspring.security.oauth2.resourceserver.jwt.jwk-set-uri=%KEYCLOAK_JWKS% ^
    -Dapp.fatourati.auth-url=%FATOURATI_AUTH_URL% ^
    -Dapp.fatourati.base-url=%FATOURATI_BASE_URL% ^
    -Dapp.fatourati.api-version=%FATOURATI_API_VERSION% ^
    -Dapp.fatourati.client-id=%FATOURATI_CLIENT_ID% ^
    -Dapp.fatourati.client-secret=%FATOURATI_CLIENT_SECRET% ^
    -Dapp.fatourati.merchant-code=%FATOURATI_MERCHANT_CODE% ^
    -Dapp.fatourati.store=%FATOURATI_STORE% ^
    -Dapp.fatourati.store-api-key=%FATOURATI_STORE_API_KEY% ^
    -Dapp.fatourati.cashier-id=%FATOURATI_CASHIER_ID% ^
    -Dapp.fatourati.callback-url=%FATOURATI_CALLBACK_URL% ^
    -Dapp.fatourati.callback-secret=%FATOURATI_CALLBACK_SECRET% ^
    -Dapp.fatourati.token-ttl-minutes=%FATOURATI_TOKEN_TTL%
