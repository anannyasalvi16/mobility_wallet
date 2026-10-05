@echo off
echo ==============================================================================
echo Stopping Mobility Wallet Platform on Port 8080...
echo ==============================================================================

powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue; Write-Host 'Stopped process ID:' $_ }"

echo.
echo Mobility Wallet is STOPPED. Port 8080 is now free.
echo ==============================================================================
echo.
pause
