@echo off
echo ==============================================================================
echo Starting Mobility Wallet in PRODUCTION Mode
echo ==============================================================================

rem 1. Check if port 8080 is already occupied; if so, free it automatically
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue; Write-Host 'Stopped old process on port 8080 (PID:' $_ ')' }"

rem 2. Set Production Environment Variables (Inherits from system if already defined)
set SPRING_PROFILES_ACTIVE=prod
if "%PORT%"=="" set PORT=8080
if "%DB_URL%"=="" set DB_URL=jdbc:mysql://localhost:3306/mobility_wallet?createDatabaseIfNotExist=true^&useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=UTC
if "%DB_USERNAME%"=="" set DB_USERNAME=root
if "%DB_PASSWORD%"=="" set DB_PASSWORD=Password
if "%ADMIN_EMAIL%"=="" set ADMIN_EMAIL=prodadmin@mobility.com
if "%ADMIN_PASSWORD%"=="" set ADMIN_PASSWORD=SecureProdPassword2026!

echo Active Profile : %SPRING_PROFILES_ACTIVE%
echo HTTP Port      : %PORT%
echo Database URL   : %DB_URL%
echo Boot Admin     : %ADMIN_EMAIL%
echo ==============================================================================

rem 3. Ensure executable JAR exists
if not exist "target\mobility-wallet-1.0.0-SNAPSHOT.jar" (
    echo.
    echo [ERROR] Application package target\mobility-wallet-1.0.0-SNAPSHOT.jar not found!
    echo Please build the package first with: cmd /c "mvn package -DskipTests"
    echo.
    pause
    exit /b 1
)

rem 4. Launch Application
java -jar target\mobility-wallet-1.0.0-SNAPSHOT.jar

rem 5. If Java stops or encounters an error, keep window open so you can read it
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ==============================================================================
    echo [ERROR] Application stopped with exit code %ERRORLEVEL%.
    echo ==============================================================================
    pause
)
