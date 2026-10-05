# 🚀 Mobility Wallet Platform - Production Deployment Guide

This guide provides step-by-step instructions for deploying the **Mobility Wallet Platform** to cloud hosting platforms (**Railway** and **Render**) using Docker and the production profile (`prod`).

---

## 📋 Table of Contents
1. [Architecture Overview](#1-architecture-overview)
2. [Environment Variables Reference](#2-environment-variables-reference)
3. [Deploying to Railway](#3-deploying-to-railway)
4. [Deploying to Render](#4-deploying-to-render)
5. [Docker Multi-Stage Deployment](#5-docker-multi-stage-deployment)
6. [Local Production Smoke-Testing](#6-local-production-smoke-testing)
7. [Production Security & Hardening Checklist](#7-production-security--hardening-checklist)

---

## 1. Architecture Overview

```
                      HTTPS / TLS
  [ Commuters & Admins ] ────────► [ Reverse Proxy / Cloud Router ]
                                         │  (Terminates SSL, sets X-Forwarded-*)
                                         ▼
                             [ Mobility Wallet App ]
                             • Spring Boot 3.2.4 (Java 17 JRE Alpine)
                             • Non-root user (appuser:appgroup)
                             • Profile: prod (application-prod.properties)
                             • Health Probe: /actuator/health
                                         │
                                         ▼
                             [ Managed MySQL 8.0 ]
                             • InnoDB, UTF8MB4
                             • ddl-auto=update (Preserves records)
```

- **Runtime:** Eclipse Temurin 17 JRE on Alpine Linux (multi-stage Docker build).
- **Security:** Non-root execution (`USER appuser`), no stack traces in error pages, CORS/CSRF enforcement, reverse proxy forward-headers handling.
- **Database:** MySQL 8.0+. All tables and relationships are auto-managed with `spring.jpa.hibernate.ddl-auto=update`.

---

## 2. Environment Variables Reference

Configure these variables in your hosting provider's dashboard or CLI:

| Environment Variable | Required? | Default / Example Value | Description |
| :--- | :---: | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | **YES** | `prod` | Activates production configuration (`application-prod.properties`). |
| `PORT` | Optional | `8080` (Injected automatically by Railway/Render) | HTTP port the application listens on. |
| `DB_URL` | **YES** | `jdbc:mysql://host:port/database?useSSL=true&serverTimezone=UTC` | JDBC connection URL to your production MySQL instance. |
| `DB_USERNAME` | **YES** | `root` or dedicated DB user | Database username with read/write/DDL privileges. |
| `DB_PASSWORD` | **YES** | `[StrongDatabasePassword]` | Password for the database user. |
| `ADMIN_EMAIL` | **YES** | `admin@yourdomain.com` | Email address for initial production administrator account. |
| `ADMIN_PASSWORD` | **YES** | `[Min12CharsStrongPass!]` | Password for initial production administrator account. |
| `ADMIN_NAME` | Optional | `System Administrator` | Display name of the initial production admin. |
| `ADMIN_PHONE` | Optional | `9876543210` | Contact phone number for the admin account. |

> [!WARNING]
> In the `prod` profile, default development credentials (`admin123` / `user123`) and demo accounts (`user@mobility.com`) are **strictly disabled**. You **must** provide `ADMIN_EMAIL` and `ADMIN_PASSWORD` to bootstrap the first administrator.

---

## 3. Deploying to Railway

Railway provides first-class support for Dockerized Spring Boot apps and managed MySQL.

### Step 1: Create a Railway Project
1. Log in to [Railway](https://railway.app/).
2. Click **"+ New Project"** ➔ **"Provision MySQL"**.
3. Railway will provision a dedicated MySQL 8 instance.

### Step 2: Deploy Mobility Wallet
1. In the same Railway project canvas, click **"+ New"** ➔ **"GitHub Repo"**.
2. Select your `mobility-wallet` repository.
3. Railway automatically detects the root `Dockerfile` and configures the multi-stage build.

### Step 3: Connect Database & Set Variables
In your web service's **Variables** tab on Railway, add:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
ADMIN_EMAIL=ops-admin@yourdomain.com
ADMIN_PASSWORD=YourStrongSecurePassword2026!
```

*(Railway automatically injects `${PORT}` and links `${{MySQL.*}}` reference variables).*

### Step 4: Configure Health Check Path
1. In your Web Service settings, navigate to **Deploy** ➔ **Healthcheck Path**.
2. Set Healthcheck Path to:
   ```
   /actuator/health
   ```
3. Set Timeout to `10` seconds.

### Step 5: Generate Public Domain
1. In **Settings** ➔ **Networking**, click **"Generate Domain"** (e.g., `mobility-wallet-production.up.railway.app`).
2. Once the build completes and healthcheck passes, open the domain in your browser.
3. Log in with `ADMIN_EMAIL` and `ADMIN_PASSWORD` at `/login`.

---

## 4. Deploying to Render

Render supports zero-configuration Docker Web Services and HTTPS proxy handling.

### Step 1: Set Up MySQL Database
Render does not offer built-in managed MySQL, so you have two options:
- **Option A (Recommended):** Use a free managed MySQL from [Aiven](https://aiven.io/), [PlanetScale](https://planetscale.com/), or [Clever Cloud](https://www.clever-cloud.com/).
- **Option B:** Deploy a MySQL Docker container on Render as a Private Service.

Obtain your MySQL connection parameters: Host, Port, Database name, User, and Password.

### Step 2: Create a New Web Service on Render
1. Log in to [Render](https://render.com/).
2. Click **"New +"** ➔ **"Web Service"**.
3. Connect your GitHub repository.
4. Set the following settings:
   - **Environment:** `Docker`
   - **Branch:** `main` (or your active branch)
   - **Plan:** Free or Starter

### Step 3: Configure Environment Variables
Scroll to **Environment Variables** and add:

```env
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=jdbc:mysql://<your-db-host>:<port>/<dbname>?useSSL=true&serverTimezone=UTC
DB_USERNAME=<your-db-username>
DB_PASSWORD=<your-db-password>
ADMIN_EMAIL=admin@yourdomain.com
ADMIN_PASSWORD=YourStrongSecurePassword2026!
```

### Step 4: Health Check Configuration
1. Expand **Advanced**.
2. In **Health Check Path**, enter:
   ```
   /actuator/health
   ```
3. Click **"Create Web Service"**.

Render will build the Docker container and deploy the app behind its managed reverse proxy.

---

## 5. Docker Multi-Stage Deployment

You can build and run the Docker image anywhere (e.g., AWS ECS, Google Cloud Run, DigitalOcean, self-hosted Linux):

### Build Docker Image
```bash
docker build -t mobility-wallet:1.0.0 .
```

### Run Container Locally or on VPS
```bash
docker run -d \
  --name mobility-wallet \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/mobility_wallet?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="YourDbPassword" \
  -e ADMIN_EMAIL="admin@prod.com" \
  -e ADMIN_PASSWORD="MySuperSecurePass2026!" \
  mobility-wallet:1.0.0
```

---

## 6. Local Production Smoke-Testing

To test the production profile on your local machine without Docker:

### 1. Build the production package
```powershell
mvn clean package -DskipTests
```

### 2. Run with production profile
Set your environment variables and start the JAR:

**Windows PowerShell:**
```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:PORT="8080"
$env:DB_URL="jdbc:mysql://localhost:3306/mobility_wallet?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="Password"
$env:ADMIN_EMAIL="prodadmin@mobility.com"
$env:ADMIN_PASSWORD="SecureProdAdminPass2026!"

java -jar target/mobility-wallet-1.0.0-SNAPSHOT.jar
```

### 3. Verify Health Check
Open another terminal or browser:
```bash
curl -i http://localhost:8080/actuator/health
```
**Expected Response:**
```json
HTTP/1.1 200 OK
Content-Type: application/vnd.spring-boot.actuator.v3+json

{"status":"UP"}
```

### 4. Verify Admin Login
1. Open `http://localhost:8080/login` in your browser.
2. Sign in with:
   - **Email:** `prodadmin@mobility.com`
   - **Password:** `SecureProdAdminPass2026!`
3. You will be redirected to the Admin Analytics Dashboard at `/admin/dashboard`.
4. Note that the demo commuter (`user@mobility.com`) does **not** exist in this database unless registered.

---

## 7. Production Security & Hardening Checklist

- [x] **Zero Hardcoded Secrets:** No database passwords or default admin credentials exist in `application-prod.properties`.
- [x] **Dev Credential Guard:** `DataInitializer` explicitly aborts seeding if default dev credentials (`admin123` / `user123`) are supplied in `prod`.
- [x] **No Demo Accounts:** Commuter demo accounts and test vouchers are completely disabled in `prod`.
- [x] **Error Leaks Suppressed:** `server.error.include-stacktrace=never`, `server.error.include-message=never`, `server.error.include-exception=false`.
- [x] **HTTPS Proxy Support:** `server.forward-headers-strategy=framework` handles TLS offloading correctly.
- [x] **Container Hardening:** Dockerfile executes under an unprivileged `USER appuser`.
- [x] **Git & Docker Exclusions:** `.env`, `.env.*`, IDE files, and build artifacts excluded in `.gitignore` and `.dockerignore`.
- [x] **Standard Health Probes:** `/actuator/health` open for container orchestration health checks without exposing sensitive system metrics.
