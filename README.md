# TinyURL Service

A production-leaning URL shortener built with Spring Boot, PostgreSQL, Redis, and ZooKeeper.

It supports:
- Creating short URLs from long URLs
- Optional custom aliases
- Redirecting `/{slug}` to the original URL
- Click-count stats per slug
- Idempotent URL creation (same normalized URL maps to same record)
- Redis caching, Bloom filter hints, resilience patterns, and optional API rate limiting

## Tech Stack

- Java 17
- Spring Boot 4 (Web, Validation, Data JPA, Data Redis, Actuator)
- PostgreSQL (source of truth)
- Redis (slug -> long URL cache)
- ZooKeeper (distributed ID allocation)
- Flyway (schema migrations)
- Resilience4j (circuit breaker + rate limiter)
- Micrometer + Prometheus registry
- Springdoc OpenAPI (Swagger UI)

## How The Application Works

1. Client sends a `POST /api/v1/urls` request with `longUrl` (and optional `customAlias`).
2. Service validates URL and normalizes + hashes it for idempotency.
3. If URL hash already exists, existing short URL is returned.
4. Otherwise:
   - For generated slugs, app reserves numeric IDs from ZooKeeper and Base62-encodes them.
   - For custom aliases, policy checks length/charset/reserved words/uniqueness.
5. Record is persisted in PostgreSQL.
6. Cache and Bloom filter are updated.
7. Redirect request `GET /{slug}` resolves URL from Redis first, falls back to DB, then increments click count.

## Prerequisites

- JDK 17
- ZooKeeper running at `localhost:2181`
- PostgreSQL running at `localhost:5432`
- Redis running at `localhost:6379`

Default local DB settings in `application.yaml`:
- database: `tinyurl`
- username: `tinyurl`
- password: `tinyurl`

## Run Locally

### 1) Start dependencies

Ensure PostgreSQL, Redis, and ZooKeeper are running on default ports:
- PostgreSQL: `5432`
- Redis: `6379`
- ZooKeeper: `2181`

### 2) Create PostgreSQL database/user (if missing)

Example SQL:

```sql
CREATE USER tinyurl WITH PASSWORD 'tinyurl';
CREATE DATABASE tinyurl OWNER tinyurl;
GRANT ALL PRIVILEGES ON DATABASE tinyurl TO tinyurl;
```

### 3) Run the application

On Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

On macOS/Linux:

```bash
./gradlew bootRun
```

App starts on: `http://localhost:8080`

Flyway auto-runs migration `V1__create_short_urls.sql` at startup.

## API Quick Start

### Create short URL

`POST /api/v1/urls`

```json
{
  "longUrl": "https://example.com/some/very/long/path",
  "customAlias": "example"
}
```

Response (`201 Created`):

```json
{
  "shortUrl": "http://localhost:8080/example",
  "slug": "example"
}
```

### Redirect

`GET /{slug}`

- Returns `302 Found` with `Location` header if slug exists
- Returns `404` if not found

### Stats

`GET /api/v1/urls/{slug}/stats`

Response:

```json
{
  "slug": "example",
  "clickCount": 12
}
```

### API docs and observability

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Actuator health/metrics/prometheus: exposed per `application.yaml`

## Profiles and Behavior

- **`test` profile** is configured in `src/main/resources/application-test.yaml` (loaded whenever `spring.profiles.active` includes `test`).
  - In-memory **H2** database with PostgreSQL compatibility mode; **Flyway** off (schema from Hibernate `create-drop`).
  - **Redis** auto-configuration excluded; actuator Redis health disabled.
  - Beans from code: `SequentialIdAllocatorConfig` (in-memory IDs), `NoopSlugCache`, and no `RedisConfiguration` / `ZookeeperConfiguration` (those classes are `@Profile("!test")`).
- **Non-`test` profile**: PostgreSQL + Flyway + Redis + ZooKeeper as in `application.yaml`.

Run the app on the test stack (no local Postgres/Redis/ZooKeeper):

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=test"
```

## Project Structure

```text
src/main/java/com/example/tinyurl
├── cache
├── config
├── controller
├── dto
├── exception
├── mapper
├── repository
├── service
│   └── id
└── web
```

## Class-By-Class Explanation

### Entry point

- `TinyurlApplication`
  - Why created: standard Spring Boot bootstrap class.
  - What it does: starts the application context and embedded web server.

### Configuration (`config`)

- `ConfigurationBeans`
  - Why created: central place to enable strongly typed config binding.
  - What it does: enables `AppProperties` with `@EnableConfigurationProperties`.

- `AppProperties`
  - Why created: avoid scattering hard-coded config values.
  - What it does: binds `app.*` settings (base URL, cache TTL, Bloom filter, ZooKeeper, rate limit).

- `RedisConfiguration`
  - Why created: explicit Redis template bean for cache service.
  - What it does: provides `StringRedisTemplate` in non-test environments.

- `ZookeeperConfiguration`
  - Why created: encapsulate ZooKeeper/Curator client setup and lifecycle.
  - What it does: creates and starts `CuratorFramework` bean in non-test profiles.

- `ResilienceConfiguration`
  - Why created: centralize resilience defaults instead of ad hoc construction.
  - What it does: creates:
    - `CircuitBreakerRegistry` used by Redis cache and ZK ID allocator
    - `RateLimiterRegistry` used by API filter

- `SequentialIdAllocatorConfig`
  - Why created: deterministic, lightweight ID generation for tests.
  - What it does: provides primary in-memory `IdAllocator` when `test` profile is active.

### Web/API layer (`controller`, `web`)

- `ShortUrlController`
  - Why created: REST API for creation and stats operations.
  - What it does:
    - `POST /api/v1/urls` -> create short URL
    - `GET /api/v1/urls/{slug}/stats` -> fetch click count
    - emits creation metric counter

- `RedirectController`
  - Why created: map human-facing short links to redirects.
  - What it does:
    - `GET /{slug}` -> resolves URL and returns `302 Found` with `Location`
    - emits redirect metric counter

- `RateLimitFilter`
  - Why created: protect API endpoints against bursts/abuse.
  - What it does: applies Resilience4j rate limiting to `/api/*` when enabled.

### Service layer (`service`, `service/id`)

- `ShortUrlService`
  - Why created: orchestration and business logic for URL shortening.
  - What it does:
    - validates input URLs
    - enforces idempotent create via URL hash
    - resolves generated/custom slug
    - persists entity
    - updates cache + Bloom filter
    - resolves redirects and increments click count

- `UrlHasher`
  - Why created: ensure stable hashing for equivalent URLs.
  - What it does: normalizes URL shape and computes SHA-256 bytes.

- `SlugPolicy`
  - Why created: enforce consistent alias rules and protect reserved routes.
  - What it does: validates custom alias format and reserved-word set.

- `IdAllocator` (interface)
  - Why created: abstract ID generation strategy from service logic.
  - What it does: defines `nextId()`.

- `ZkRangeIdAllocator`
  - Why created: distributed, collision-safe ID allocation for multi-instance deployments.
  - What it does:
    - reserves disjoint ID ranges from ZooKeeper atomic counter
    - serves IDs locally from reserved range
    - wraps failures with `IdAllocationException`
    - guarded by circuit breaker

- `Base62Encoder`
  - Why created: compact slug representation from numeric IDs.
  - What it does: encodes non-negative `long` into URL-safe Base62 text.

- `IdAllocationException`
  - Why created: explicit domain exception for allocation failures.
  - What it does: signals temporary ID allocation issues for consistent handling.

### Cache (`cache`)

- `SlugCache` (interface)
  - Why created: decouple caching strategy from core service.
  - What it does: defines read/write contract for slug-to-URL lookup.

- `UrlCacheService`
  - Why created: production cache implementation with graceful degradation.
  - What it does:
    - reads/writes Redis `tinyurl:url:{slug}` keys
    - applies configured TTL
    - fail-open behavior on Redis/circuit failures

- `NoopSlugCache`
  - Why created: test-friendly cache implementation without infrastructure dependency.
  - What it does: always cache-miss, ignores writes.

- `SlugBloomFilter`
  - Why created: probabilistic optimization to reduce expensive lookups after warmup.
  - What it does: keeps in-process Bloom filter of known slugs (optional via config).

### Persistence (`repository`)

- `ShortUrl` (entity)
  - Why created: JPA model for `short_urls` table.
  - What it does: stores id, slug, long URL, normalized hash, creation time, click count.

- `ShortUrlRepository`
  - Why created: data access abstraction.
  - What it does:
    - find by slug
    - find by URL hash
    - increment click count using update query

### DTOs (`dto`)

- `CreateShortUrlRequest`
  - Why created: validate and model creation payload.
  - What it does: carries `longUrl` and optional `customAlias` with constraints.

- `CreateShortUrlResponse`
  - Why created: stable response contract for create API.
  - What it does: returns `shortUrl` and `slug`.

- `UrlStatsResponse`
  - Why created: stable response contract for stats API.
  - What it does: returns slug + click count.

### Mapping (`mapper`)

- `ShortUrlMapper`
  - Why created: keep conversion logic out of controllers/services.
  - What it does: maps `ShortUrl` entity to API response DTOs.

### Error handling (`exception`)

- `ApiError`
  - Why created: consistent JSON error payload shape.
  - What it does: carries timestamp, status, message, and path.

- `GlobalExceptionHandler`
  - Why created: centralized exception-to-HTTP mapping.
  - What it does: maps validation/conflict/id-allocation/unhandled errors to structured responses.

### Tests (`src/test`)

- `TinyurlApplicationTests`
  - Why created: smoke test to ensure Spring context starts under `test` profile.
  - What it does: verifies application context is non-null.

- `Base62EncoderTest`
  - Why created: protect core encoding behavior from regressions.
  - What it does: validates known values and negative-input rejection.

## Running Tests

```powershell
.\gradlew.bat test
```

## Notes for Production

- Prefer environment variables over hard-coded secrets in `application.yaml`.
- Keep `app.short-url-base` set to your public domain.
- Ensure Redis, PostgreSQL, and ZooKeeper are reachable and monitored.
- Keep Flyway migrations versioned and immutable.
