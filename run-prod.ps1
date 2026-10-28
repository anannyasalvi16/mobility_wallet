# ==============================================================================
# Run Mobility Wallet Platform in PRODUCTION Profile
# ==============================================================================
$env:SPRING_PROFILES_ACTIVE = "prod"
if (-not $env:PORT) { $env:PORT = "8080" }
if (-not $env:DB_URL) { $env:DB_URL = "jdbc:mysql://localhost:3306/mobility_wallet?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" }
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "root" }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = "your_db_password" }
if (-not $env:ADMIN_EMAIL) { $env:ADMIN_EMAIL = "admin@example.com" }
if (-not $env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD = "ChangeMeInProduction2026!" }

Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "Starting Mobility Wallet in PRODUCTION Mode" -ForegroundColor Green
Write-Host "Active Profile : $env:SPRING_PROFILES_ACTIVE" -ForegroundColor Yellow
Write-Host "HTTP Port      : $env:PORT" -ForegroundColor Yellow
Write-Host "Database URL   : $env:DB_URL" -ForegroundColor Yellow
Write-Host "Boot Admin     : $env:ADMIN_EMAIL" -ForegroundColor Yellow
Write-Host "==============================================================================" -ForegroundColor Cyan

java -jar target\mobility-wallet-1.0.0-SNAPSHOT.jar
