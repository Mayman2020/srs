# Deployment - Administrative Communications System

Production-oriented Docker Compose stack: PostgreSQL 16, Spring Boot API, Angular SPA (Nginx), and an Nginx edge gateway (`/api/` to API, `/` to SPA).

## Layout

| Path | Purpose |
|------|---------|
| `docker/backend/Dockerfile` | Multi-stage Maven build plus JRE runtime |
| `docker/frontend/Dockerfile` | `npm ci`, `ng build`, and Nginx static runtime |
| `docker/frontend/nginx-spa.conf` | SPA fallback routing |
| `docker/nginx/gateway.conf` | Reverse proxy and baseline security headers |
| `compose/docker-compose.yml` | Base stack with health checks and `unless-stopped` |
| `compose/docker-compose.staging.yml` | Staging overlay |
| `compose/docker-compose.prod.yml` | Production overlay |
| Repo-root `docker-compose*.yml` | Thin wrappers so root usage matches the ERP project workflow |
| `env/*.env.example` | Required variables; copy to real env files and never commit secrets |

## Quick start

From `deploy/compose`:

```bash
cd deploy/env
cp .env.example .env

cd ../compose
docker compose --env-file ../env/.env up -d --build
```

Equivalent root-level usage:

```bash
docker compose --env-file deploy/env/.env -f docker-compose.yml up -d --build
```

Gateway URL: `http://localhost:${HTTP_PORT:-8080}`.

## Staging and production

From `deploy/compose`:

```bash
docker compose -f docker-compose.yml -f docker-compose.staging.yml --env-file ../env/staging.env up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file ../env/prod.env up -d --build
```

Using root wrappers:

```bash
docker compose --env-file deploy/env/staging.env -f docker-compose.staging.yml up -d --build
docker compose --env-file deploy/env/prod.env -f docker-compose.prod.yml up -d --build
```

## Profiles

| Profile | Config file | Notes |
|---------|-------------|-------|
| default | `application.yml` | Local-friendly defaults |
| `staging` | `application-staging.yml` | Swagger on, HSTS off by default |
| `prod` | `application-prod.yml` | Swagger/OpenAPI off, strict CORS from env |

## Secrets

- `AC_JWT_SECRET`: HS256 signing key, at least 32 UTF-8 bytes
- `POSTGRES_PASSWORD`: PostgreSQL password
- `CAMUNDA_BPM_ADMIN_PASSWORD`: Camunda admin password

## Flyway

Migrations run on backend startup. Use a persistent `pgdata` volume. Empty DB means Flyway applies `V1` through `V18` and seeds the registry data.

Application data lives in PostgreSQL schema `srs_system` (catalogue `postgres`, same layout as other apps that share one DB with multiple schemas).

## Load tests

See `tests/load/k6/README.md`. Point `BASE_URL` at a reachable API base such as `http://localhost:8080/api/v1`.
