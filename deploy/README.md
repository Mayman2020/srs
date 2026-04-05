# Deployment — Administrative Communications System

Production-oriented Docker Compose stack: **PostgreSQL 16**, **Spring Boot** API, **Angular** SPA (Nginx), **Nginx** edge gateway (`/api/` → API, `/` → SPA).

## Layout

| Path | Purpose |
|------|---------|
| `docker/backend/Dockerfile` | Multi-stage Maven build + JRE runtime (non-root `ac` user, `curl` health). |
| `docker/frontend/Dockerfile` | `npm ci` + `ng build` + Nginx static. |
| `docker/frontend/nginx-spa.conf` | SPA fallback routing. |
| `docker/nginx/gateway.conf` | Reverse proxy, upload size **55m**, baseline security headers. |
| `compose/docker-compose.yml` | Base stack + health checks + `unless-stopped`. |
| `compose/docker-compose.staging.yml` | Staging profile overlay. |
| `compose/docker-compose.prod.yml` | Production profile overlay. |
| `env/*.env.example` | Required variables (copy to real env files; never commit secrets). |

## Quick start (local Docker)

```bash
cd deploy/env
cp .env.example .env
# Edit .env — set strong POSTGRES_PASSWORD, AC_JWT_SECRET (≥32 bytes), CAMUNDA_BPM_ADMIN_PASSWORD

cd ../compose
docker compose --env-file ../env/.env up -d --build
```

Open `http://localhost:${HTTP_PORT:-8080}` (gateway). API: `http://localhost:8080/api/v1/...`.

Health (internal to Docker network, or port-forward backend): `GET http://backend:8080/actuator/health`.

## Staging / production overlays

```bash
docker compose -f docker-compose.yml -f docker-compose.staging.yml --env-file ../env/staging.env up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file ../env/prod.env up -d --build
```

**Production CORS:** `AC_CORS_ALLOWED_ORIGIN_PATTERNS` must list real HTTPS origins only. The `prod` Spring profile **fails fast** if patterns are empty or contain `localhost` / `127.0.0.1` (see `CorsConfig`).

**TLS:** Terminate TLS at Nginx or cloud load balancer. For HTTPS Nginx, add `listen 443 ssl`, certificates, and `add_header Strict-Transport-Security ...` on the edge. When TLS terminates before Spring Boot, keep `AC_SECURITY_HSTS_ENABLED=false` on the JVM (see `application-prod.yml`).

## Spring profiles

| Profile | Config file | Notes |
|---------|----------------|-------|
| (default) | `application.yml` | Local dev; permissive CORS default. |
| `staging` | `application-staging.yml` | Swagger on; HSTS off by default in overlay. |
| `prod` | `application-prod.yml` | Swagger/OpenAPI **off**; CORS from env **required**; actuator **health** only. |

## Secrets (never commit)

- `AC_JWT_SECRET` — HS256 signing key; **≥ 32 UTF-8 bytes**. Rotation: issue new secret, redeploy (all sessions invalidate); document overlap window if using blue/green dual-key (not implemented in code — single secret today).
- `POSTGRES_PASSWORD`, `CAMUNDA_BPM_ADMIN_PASSWORD` — store in vault / CI secrets.

## Flyway

Migrations run on backend startup. Use a **persistent** `pgdata` volume (already defined). Empty DB → Flyway applies `V1`–`V15` + seed.

## k6 load tests

See `tests/load/k6/README.md`. Point `BASE_URL` at a reachable API base (e.g. `http://localhost:8080/api/v1` for local JVM, or gateway `/api/v1`).
