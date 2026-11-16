# Khmer Wallet API — Multi-Currency Wallet (USD / KHR)

[![CI](https://github.com/seyhakbotr/khmer-wallet-api/actions/workflows/ci.yml/badge.svg)](https://github.com/seyhakbotr/khmer-wallet-api/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)

A small, boring-on-purpose backend that solves a real problem: **holding and moving money in two currencies without ever losing a cent**.


## Stack

| Piece | What |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 (Spring MVC + Spring Security) |
| Database | PostgreSQL 16 (schema managed by Flyway migrations) |
| Auth | JWT bearer tokens, BCrypt password hashing |
| Build | Gradle (wrapper included) |
| Tests | JUnit 5 + MockMvc + Testcontainers (real Postgres in Docker) |
| Runtime | Docker / Docker Compose |

## What works today

- Register / log in with a Cambodian phone number (`012345678` or `+85512345678`)
- JWT-protected endpoints (stateless — no server sessions)
- Create a USD or KHR account per user (one of each, max)
- List your own accounts
- One consistent JSON error shape for validation, 401, 404 and 409

## Running it

### Everything in Docker (one command)

```bash
docker compose up --build
# API: http://localhost:8080
```

### Local development

```bash
# 1. Start only the database
docker compose up -d db

# 2. Run the app (uses localhost:5434 by default, so it won't clash with a local Postgres)
./gradlew bootRun
```

### Tests

```bash
./gradlew test
```

Tests need Docker: Testcontainers starts a throwaway Postgres 16 container, applies the Flyway migrations, and runs real HTTP-level tests through the security filter chain.

## Try it

```bash
# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Dara Sok","phone":"012345678","password":"password123"}'

# Log in and copy the token
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"phone":"012345678","password":"password123"}'

# Create a USD account
curl -s -X POST http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"currency":"USD"}'

# List my accounts
curl -s http://localhost:8080/api/accounts -H "Authorization: Bearer $TOKEN"

# Who am I?
curl -s http://localhost:8080/api/me -H "Authorization: Bearer $TOKEN"
```

## Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | — | Create a user |
| POST | `/api/auth/login` | — | Get a JWT |
| GET | `/api/me` | Bearer | Current user |
| POST | `/api/accounts` | Bearer | Create USD/KHR account |
| GET | `/api/accounts` | Bearer | List my accounts |

Errors always look like this:

```json
{
  "timestamp": "2026-11-16T16:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/auth/register",
  "fieldErrors": { "phone": "must be a Cambodian phone number, e.g. 012345678 or +85512345678" }
}
```

## Design decisions

1. **Money is an integer, never a float.** `Money` stores minor units (`long`): USD in cents, KHR in sen. `0.1 + 0.2 != 0.3` in floating point; cents arithmetic is exact. `Money` refuses to mix currencies.
2. **Accounts have no `balance` column.** Balance will be **derived** from an append-only double-entry ledger (next phase), so it can always be audited. A stored counter can silently drift; history cannot.
3. **Phone number is the username.** Matches how Wing/ABA-style wallets work in Cambodia where the phone number is the familiar login identifier.
4. **Stateless JWT auth.** Each request carries a signed token; no server-side session store. BCrypt (with per-password salt) protects the password hashes.
5. **One account per user per currency** — enforced by a DB unique constraint, not just application code. Two layers, because races happen.
6. **Flyway migrations** are Java's version of Laravel migrations: every schema change is a numbered, committed SQL file (`db/migration/V1__init.sql`).
7. **Consistent error contract.** A single `@RestControllerAdvice` translates exceptions into one predictable JSON shape; clients (and tests) can rely on it.
8. **Integration tests hit a real database.** H2 would hide Postgres behavior (constraints, identity columns, locking). Testcontainers gives production parity.

## Project layout

```
src/main/java/com/wallet/api/
├── auth/        register + login (DTOs, service, controller)
├── account/     create/list accounts
├── security/    JWT service, filter, Spring Security config
├── user/        User entity + repository, /api/me
└── common/      Currency, Money, shared error handling
src/main/resources/
├── application.yml
└── db/migration/V1__init.sql
src/test/java/com/wallet/api/
├── AbstractIntegrationTest.java   real Postgres + MockMvc base class
├── auth/AuthControllerIT.java
├── account/AccountControllerIT.java
└── common/MoneyTest.java
```

## Roadmap

- [x] **Phase 1** — users, JWT auth, accounts, validation, errors, tests
- [ ] **Phase 2** — deposit / withdraw, `Money` used end-to-end, insufficient-funds rule
- [ ] **Phase 3** — transfers, double-entry ledger, idempotency keys, pessimistic locking, audit log
- [ ] **Phase 4** — daily transfer limits, end-of-day statements (`@Scheduled`), USD/KHR rate table
- [ ] **Phase 5** — Swagger UI, full README + demo video

## Configuration

| Env var | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5434/wallet` | host port 5434 avoids clashing with a local Postgres |
| `DB_USER` / `DB_PASSWORD` | `wallet` / `wallet` | dev only |
| `JWT_SECRET` | dev-only default | must be ≥ 32 bytes; **override in any real environment** |
| `JWT_EXPIRATION_MINUTES` | `60` | |
| `SERVER_PORT` | `8080` | |
