# Coupon Service

REST service for managing discount coupons: create a coupon, register its usage
(usage limit, country restriction based on the client's IP, one usage per user).

Stack: Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway, Maven.

## Run

```bash
docker compose up -d      # PostgreSQL on localhost:5432
mvn spring-boot:run       # http://localhost:8080
mvn verify                # tests (integration tests need Docker)
```

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/coupons` | create a coupon `{"code","maxUsages","country"}` |
| `GET` | `/api/coupons/{code}` | show a coupon incl. `remainingUsages` (code is case-insensitive) |
| `POST` | `/api/coupons/{code}/redemptions` | redeem; body `{"userId"}`, client IP taken from the request |

| Outcome | HTTP | `reason` |
|---|---|---|
| redeemed | 201 | – |
| coupon does not exist | 404 | `COUPON_NOT_FOUND` |
| client country differs from the coupon's (or is unknown) | 403 | `COUNTRY_NOT_ALLOWED` |
| usage limit reached | 409 | `COUPON_LIMIT_REACHED` |
| user already redeemed this coupon | 409 | `COUPON_ALREADY_REDEEMED` |
| code already taken (case-insensitive) | 409 | `COUPON_CODE_ALREADY_EXISTS` |
| geolocation: all providers failed | 503 | `GEOLOCATION_UNAVAILABLE` |
| too many attempts from one client | 429 (+`Retry-After`) | `RATE_LIMITED` |
| `Idempotency-Key` reused with a different request | 422 | `IDEMPOTENCY_KEY_REUSED` |
| duplicate of a request still being processed | 409 | `REQUEST_IN_PROGRESS` |
| invalid input | 400 | `VALIDATION_FAILED` |

Errors are RFC 7807 `application/problem+json` with an extra machine-readable `reason`.
Locally the client IP is 127.0.0.1 and cannot be geolocated; simulate a country with
`X-Forwarded-For: <public IP>` (localhost is a trusted proxy).

## Structure

```
com.example.coupons
├── domain                      pure Java, no framework imports
│   ├── model                   Coupon (aggregate root), Redemption, value objects:
│   │                           CouponId, CouponCode, CountryCode, UserId
│   ├── event                   domain events: CouponCreated, CouponRedeemed + DomainEventPublisher (port)
│   ├── exception               business-rule violations
│   ├── repository              ports: CouponRepository, RedemptionRepository
│   ├── rule                    redemption rule chain: CountryRule, AlreadyRedeemedRule
│   └── service                 CouponRedeemer (domain service, runs the rule chain)
├── application
│   ├── command                 WRITE side: CreateCouponHandler, RedeemCouponHandler,
│   │                           IdempotentRedeemCouponHandler + commands, IdempotencyStore (port)
│   ├── query                   READ side: GetCouponHandler, CouponReadModel (port), CouponView
│   └── port                    outbound port: ClientCountryResolver
├── infrastructure              adapters (depend inward, never the other way)
│   ├── persistence             JPA entities + repository adapters (write) and JdbcCouponReadModel (read)
│   ├── geo                     ip-api.com and ipapi.co adapters, circuit breaker, fallback chain, cache
│   ├── outbox                  transactional outbox: publisher, relay, sink
│   ├── idempotency             JdbcIdempotencyStore
│   └── config                  wiring of framework-free domain services, scheduled jobs
└── interfaces.rest             controller, request/response DTOs, exception → HTTP mapping
    └── ratelimit               token-bucket rate limiter (per client IP) + interceptor
```

Dependency direction: `interfaces → application → domain ← infrastructure`.
`ArchitectureTest` (ArchUnit) enforces it: e.g. the domain may not import Spring, JPA, Jackson
or any outer layer, and the web layer may not touch persistence.

## DDD building blocks

| Concept | Where | Reasoning |
|---|---|---|
| **Aggregate root** | `Coupon` | Owns the invariants of one coupon: usage count never exceeds the limit; usable only from its country. State changes only through `redeem(...)`. |
| **Value objects** | `CouponCode`, `CountryCode`, `UserId`, `CouponId` | Self-validating and immutable. Normalization of codes ("WIOSNA" = "wiosna") lives in one place; an invalid country code cannot exist. |
| **Domain service** | `CouponRedeemer` | "One user, one redemption" spans the aggregate and the history of redemptions, so it belongs to neither alone. |
| **Repository ports** | `domain.repository` | The domain states what it needs; infrastructure provides it. |
| **Command handlers** | `application.command` | One handler per use case: transactions, ports, orchestration. No business rules. |
| **Query handlers** | `application.query` | One handler per question; read a projection, never an aggregate. |
| **Domain exceptions** | `domain.exception` | Business outcomes as named exceptions, mapped to HTTP in one place. A failed redemption rolls the transaction back automatically. |
| **Separate persistence model** | `CouponJpaEntity` | The domain model is not shaped by JPA. The cost is a small mapping layer. |
| **Anti-corruption / port** | `ClientCountryResolver` | The application speaks in `CountryCode`; ip-api.com's JSON stays inside the adapter. |

## Concurrency design

1. The application service loads the coupon with `findByCodeForUpdate` = `SELECT ... FOR UPDATE`.
   Redemptions of **one** coupon are serialized by the database, across threads **and** application instances.
2. Inside that lock: duplicate check (`CouponRedeemer`) → `coupon.redeem(...)` → `update` + save the `Redemption`.
3. Any domain exception rolls back the transaction and releases the lock.
4. Database constraints remain as a backstop: `UNIQUE (code)`, `UNIQUE (coupon_id, user_id)`,
   `CHECK (usage_count <= max_usages)`.

The external geolocation call happens **outside** the transaction, so a slow provider never holds a
connection or a row lock.

## CQRS

Commands and queries are separate models with separate code paths:

| | Write side (commands) | Read side (queries) |
|---|---|---|
| Entry points | `POST /api/coupons`, `POST .../redemptions` | `GET /api/coupons/{code}` |
| Handlers | `CreateCouponHandler`, `RedeemCouponHandler` | `GetCouponHandler` |
| Model | domain: `Coupon` aggregate, repositories | `CouponView` projection |
| Data access | JPA, pessimistic lock, transactions | plain SQL via `JdbcClient`, read-only, no locks |
| Returns | minimal result (code / receipt) | the representation |

Points worth knowing:

- **The read model is not the aggregate.** `CouponView` has `remainingUsages`, which the aggregate neither stores
  nor needs. Reads never load `Coupon`, never take locks, and so never slow down redemptions.
- **Commands do not return read models.** `POST /api/coupons` returns the key from the command and the controller
  fetches the representation through the query side.
- **Separation is enforced.** `ArchitectureTest` fails the build if the command side uses the query side,
  or if the query side touches the aggregate, repositories or domain services.
- **Handlers are plain beans, no mediator/bus.** `CommandHandler` and `QueryHandler` are small interfaces;
  controllers inject the handlers they need. A bus would add indirection with no benefit at this size.

## Patterns used in project

| Pattern | Where | What it solves |
|---|---|---|
| **Circuit breaker** (Resilience4j) | `CircuitBreakingClientCountryResolver` | A failing geo provider is cut off after the failure threshold, so threads don't wait for timeouts and the provider isn't hammered. Only "unavailable" counts as failure; an empty answer ("cannot locate") does not. |
| **Fallback chain** | `FallbackClientCountryResolver` | Tries ip-api.com, then ipapi.co, behind the same `ClientCountryResolver` port. Full chain: cache -> fallback -> (breaker -> provider) x2. |
| **Rule chain** | `domain.rule.*`, `CouponRedeemer` | Country and "one per user" are separate `RedemptionRule`s; a new policy (expiry, ...) is a new class registered in `DomainConfiguration`. The usage-limit invariant stays in the aggregate. |
| **Domain events + transactional outbox** | `Coupon.pullEvents`, `OutboxDomainEventPublisher`, `OutboxRelay` | `CouponCreated` / `CouponRedeemed` are written in the same transaction as the state change (`Propagation.MANDATORY`) and relayed later, at-least-once, with `FOR UPDATE SKIP LOCKED` so several instances can relay. The sink only logs; Kafka/RabbitMQ would replace it. |
| **Idempotency key** | `IdempotentRedeemCouponHandler`, `JdbcIdempotencyStore` | `Idempotency-Key` header (optional): a retry gets the original receipt. Only successes are stored; failures release the key; same key + different request -> 422. |
| **Rate limiter** (token bucket) | `interfaces.rest.ratelimit` | Per-client (IP) burst + sustained limit on redemption attempts, 429 with `Retry-After`. |
| **Test Data Builder** | `CouponBuilder` (tests) | `aCoupon().withLimit(1).inCountry("DE").build()`. |

Known limits, on purpose:
- The rate limiter is **in memory per instance**; a strict cluster-wide limit needs a shared store (Redis).
- Outbox delivery is **at-least-once**; consumers must tolerate duplicates and must not rely on ordering.
- The idempotency store keeps successful receipts for `retention` (24 h) and purges them hourly.
- The fallback provider (ipapi.co) has its own free-tier limits; GeoLite2 would be the production answer.
- Unit tests for the circuit breaker, fallback, rules, token bucket and idempotency need no Docker; the outbox, idempotency and rate-limit integration tests do.

## Tests

Unit tests (no Docker needed):

- **Domain** (`CouponTest`, `CouponRedeemerTest`, `CouponCodeTest`, `CountryRuleTest`, `AlreadyRedeemedRuleTest`):
  pure Java, run in milliseconds, no Spring, no DB. Coupons are built with `CouponBuilder`.
- **`ArchitectureTest`**: layering and CQRS separation rules (ArchUnit).
- **Geolocation** (`IpApiClientCountryResolverTest`, `IpApiCoClientCountryResolverTest`): adapters against a mocked HTTP server;
  `CircuitBreakingClientCountryResolverTest`, `FallbackClientCountryResolverTest`: breaker opening and provider fallback.
- **`IdempotentRedeemCouponHandlerTest`**: replay, key reuse with a different request, key release on failure.
- **`TokenBucketTest`**: burst, refill and `Retry-After` calculation.

Integration tests (real PostgreSQL via Testcontainers, geolocation mocked):

- **`CouponApiIntegrationTest`**: full HTTP flow and error mapping.
- **`CouponRedemptionConcurrencyTest`**: 40 threads on a coupon with limit 5 → exactly 5 successes;
  20 parallel requests from one user → 1 success; concurrent creation of the same code → 1 success.
- **`OutboxIntegrationTest`**: events are stored with the state change and relayed.
- **`IdempotencyIntegrationTest`**: `Idempotency-Key` behaviour over HTTP.
- **`RateLimitIntegrationTest`**: 429 with `Retry-After` once the limit is exceeded.

## Deliberate omissions

- **Event sourcing and a separate read store**: not justified here; CQRS is applied in its lightweight form (see above).
- **Message broker**: the outbox sink only logs; a Kafka/RabbitMQ sink would replace `LoggingOutboxEventSink`.
- **Authentication**, OpenAPI, metrics: out of scope for the task.
