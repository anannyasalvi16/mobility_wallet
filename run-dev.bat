@echo off
echo ==============================================================================
echo Starting Mobility Wallet in DEVELOPMENT Mode (mvn spring-boot:run)
echo ==============================================================================

rem Check if port 8080 is already occupied; if so, free it automatically
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue; Write-Host 'Stopped old process on port 8080 (PID:' $_ ')' }"

cmd.exe /c "mvn spring-boot:run"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ==============================================================================
    echo [ERROR] Maven exited with code %ERRORLEVEL%.
    echo ==============================================================================
    pause
)
