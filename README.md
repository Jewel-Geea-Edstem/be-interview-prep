# be-interview-prep

Five Spring Boot features built as one application. Each feature was built on its own branch, reviewed, and merged as its own pull request.

Stack: Java 21, Spring Boot 3.5, Spring Data JPA, Spring Security (JWT resource server), Caffeine cache, H2 (in memory), Maven wrapper.

## Prerequisites

- Java 21 (Spring Boot 3.5 needs Java 17 or newer)
- Docker (optional)

There is no database to install: the app uses an in-memory H2 database, and 100 products are seeded on startup.

## Configuration

| Variable | Required | Purpose |
|----------|----------|---------|
| `JWT_SECRET` | yes | HS256 signing key, at least 32 bytes. The app refuses to start without it. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | no | Seed an ADMIN user on startup |
| `JWT_TTL` | no | Token lifetime (default `15m`) |
| `SHORT_URL_BASE` | no | Base of generated short URLs (default `http://localhost:8080`) |
| `PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | no | Server port and datasource overrides |

See `.env.example`.

## Run

```bash
JWT_SECRET=local-run-secret-placeholder-0123456789abcdef \
ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=admin-pass-123 \
./mvnw spring-boot:run
```

Or with Docker:

```bash
docker build -t be-interview-prep .
docker run -p 8080:8080 -e JWT_SECRET=local-run-secret-placeholder-0123456789abcdef \
  -e ADMIN_EMAIL=admin@example.com -e ADMIN_PASSWORD=admin-pass-123 be-interview-prep
```

- Health check: `curl http://localhost:8080/actuator/health`
- API docs: http://localhost:8080/swagger-ui.html

## Test

```bash
./mvnw verify
```

Tests run against H2 with a test-only JWT secret, so no setup is needed.

## Code formatting

The code is formatted with Spotless and google-java-format, which also removes unused imports.

```bash
./mvnw spotless:apply   # format
./mvnw spotless:check   # fail if anything is unformatted
```

Full local gate: `./mvnw -B spotless:apply verify`.

## Calling the API

Every `/api/**` endpoint except `/api/v1/auth/**` needs `Authorization: Bearer <token>`. Short-link redirects (`/r/{code}`) are public.

```bash
curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' -d '{"email":"alice@example.com","password":"alice-pass-1"}'
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' -d '{"email":"alice@example.com","password":"alice-pass-1"}' | jq -r .accessToken)
```

| Feature | Method and path | Example |
|---------|-----------------|---------|
| Tasks | `POST/GET /api/v1/tasks`, `GET/PUT/DELETE /api/v1/tasks/{id}`, `?status=` | `curl -H "Authorization: Bearer $TOKEN" "localhost:8080/api/v1/tasks?status=TODO"` |
| URL shortener | `POST /api/v1/urls`, `GET /r/{code}`, `GET /api/v1/urls/{code}/stats` | `curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"url":"https://example.com"}' localhost:8080/api/v1/urls` |
| Auth | `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/users/me`, `GET /api/v1/users` (ADMIN) | `curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/users/me` |
| Products | `GET /api/v1/products?category=&minPrice=&maxPrice=&inStock=&q=&sort=&page=&size=`, `GET /api/v1/products/{id}`, `POST/PUT/DELETE` (ADMIN) | `curl -H "Authorization: Bearer $TOKEN" "localhost:8080/api/v1/products?category=books&inStock=true&sort=price,desc"` |
| Orders | `POST /api/v1/orders` (header `Idempotency-Key`), `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel` | `curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: order-1' -H 'Content-Type: application/json' -d '{"items":[{"productId":1,"quantity":1}]}' localhost:8080/api/v1/orders` |

Every error uses one JSON shape: `{status, error, message, path, timestamp, details[]}`.

## Design notes

- **Concurrency:**
  - Visit counts (Q2) and stock reservations (Q5) are single atomic SQL `UPDATE`s, never read-then-write.
  - Orders are all-or-nothing within one transaction.
  - Order retries are recognised by an idempotency key that is unique per customer.
  - See `docs/q5-concurrency.md` for the alternatives considered.
- **Caching (Q4):** single-product lookups are cached in Caffeine. Evictions happen after commit, and `sync = true` loading prevents a concurrent read from re-caching stale data.
- **Security (Q3):** stateless JWT (HS256, 15 minutes), BCrypt passwords, and 401/403 returned as JSON. The secret comes only from the environment.

## Trade-offs and what's next

- H2 in memory keeps setup at zero. Next steps: Postgres with Flyway migrations, and Testcontainers for tests.
- The cache is per instance. Running several instances would need Redis or invalidation messages between them.
- No refresh tokens or logout yet. Idempotency keys never expire; a cleanup job is needed.

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | [#1](https://github.com/Jewel-Geea-Edstem/be-interview-prep/pull/1) |
| 2 | URL Shortener | [#2](https://github.com/Jewel-Geea-Edstem/be-interview-prep/pull/2) |
| 3 | Authentication & Roles | [#3](https://github.com/Jewel-Geea-Edstem/be-interview-prep/pull/3) |
| 4 | Product Catalog | [#4](https://github.com/Jewel-Geea-Edstem/be-interview-prep/pull/4) |
| 5 | Order Service | [#5](https://github.com/Jewel-Geea-Edstem/be-interview-prep/pull/5) |

Video:
