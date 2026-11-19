# Khmer Wallet API — Multi-Currency Wallet (USD / KHR)

[![CI](https://github.com/seyhakbotr/khmer-wallet-api/actions/workflows/ci.yml/badge.svg)](https://github.com/seyhakbotr/khmer-wallet-api/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)

A small, correctness-first backend that solves a real problem: **holding and moving money in two currencies without ever losing a cent**. Every movement is recorded in an append-only ledger, and balances are derived from it.

**Try it in the browser:** start the app and open <http://localhost:8080/swagger-ui.html>.

## Stack

| Piece | What |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 (Spring MVC + Spring Security) |
| Database | PostgreSQL 16 (schema managed by Flyway migrations) |
| Auth | JWT bearer tokens, BCrypt password hashing |
| Build | Gradle (wrapper included) |
| Tests | JUnit 5 + MockMvc + Testcontainers (real Postgres in Docker) |
| API docs | springdoc-openapi + Swagger UI (OpenAPI 3) |
| Runtime | Docker / Docker Compose |

## What works today

- Register / log in with a Cambodian phone number (`012345678` or `+85512345678`)
- JWT-protected endpoints (stateless — no server sessions)
- Create a USD or KHR account per user (one of each, max)
- List your own accounts
- Deposit and withdraw money with exact minor-unit arithmetic
- Transfer money to another account in the same currency, with **double-entry** ledger rows (debit + credit = 0)
- Safe retries with an `Idempotency-Key` header — the same key moves money once
- Pessimistic row locks so two concurrent requests cannot overdraw the same account
- An append-only audit log enforced by database triggers (no UPDATE, DELETE or TRUNCATE)
- Daily outgoing-transfer limits per account, in each currency
- Nightly `@Scheduled` statements with opening/closing balances and per-type totals
- An admin-set USD/KHR rate table with dated history; users can only read the current rate
- Read a balance derived from the append-only ledger (`SUM(entries)`, never a stored counter)
- Interactive Swagger UI with a bearer-token **Authorize** button
- One consistent JSON error shape for validation, 401, 404, 409 and 422

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

### API docs

Start the app, then open:

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>
- **Raw OpenAPI:** <http://localhost:8080/v3/api-docs>

Both are public. Click **Authorize** and paste the JWT returned by `/api/auth/login` to try
protected endpoints. The public register/login operations are marked as requiring no token.

## Testing

`./gradlew test` runs the whole suite against a real Postgres started by Testcontainers.

- **Unit tests** cover the pure rules: `Money` parsing/arithmetic/currency safety, ledger
  entry signs, transfer validation, and limit configuration.
- **Integration tests** drive the real HTTP + security + Flyway + Postgres stack: auth and
  validation, accounts, deposits/withdrawals, transfers, idempotency, concurrency, daily
  limits, statements, rates/roles, and the OpenAPI endpoints.
- The two concurrency tests use two threads and assert that exactly one full-balance
  request succeeds, which is the evidence that the pessimistic lock works.

Current suite: **112 tests, 0 failures, 0 skipped**.

## Try it

```bash
# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Dara Sok","phone":"012345678","password":"password123"}'

# Log in and capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"phone":"012345678","password":"password123"}' \
  | sed 's/.*"token":"\([^"]*\)".*/\1/')

# Create a USD account and capture its account number
ACC=$(curl -s -X POST http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"currency":"USD"}' \
  | sed 's/.*"accountNumber":"\([^"]*\)".*/\1/')

# Deposit 10.50
curl -s -X POST "http://localhost:8080/api/accounts/$ACC/deposits" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":"10.50"}'

# Withdraw 4.00
curl -s -X POST "http://localhost:8080/api/accounts/$ACC/withdrawals" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":"4.00"}'

# Balance: 6.50
curl -s "http://localhost:8080/api/accounts/$ACC/balance" \
  -H "Authorization: Bearer $TOKEN"

# Transfer 2.00 to another account in the same currency.
# Replace 9000000001 with a real destination account number.
# Repeating the same Idempotency-Key returns the original result and moves money once.
curl -s -X POST http://localhost:8080/api/transfers \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Idempotency-Key: demo-transfer-001' \
  -d "{\"sourceAccountNumber\":\"$ACC\",\"destinationAccountNumber\":\"9000000001\",\"amount\":\"2.00\"}"

# List my accounts
curl -s http://localhost:8080/api/accounts -H "Authorization: Bearer $TOKEN"

# Current USD/KHR rate (KHR per 1 USD)
curl -s http://localhost:8080/api/rates/usd-khr -H "Authorization: Bearer $TOKEN"

# Statements generated by the nightly job (00:05 Asia/Phnom_Penh)
curl -s "http://localhost:8080/api/accounts/$ACC/statements" \
  -H "Authorization: Bearer $TOKEN"

# Admin only: publish a rate. Promote a user first:
# docker compose exec db psql -U wallet -d wallet \
#   -c "update users set role = 'ADMIN' where phone = '012345678';"
curl -s -X PUT http://localhost:8080/api/admin/usd-khr-rates \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"rate":"4150.00","effectiveFrom":"2026-11-19"}'

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
| POST | `/api/accounts/{accountNumber}/deposits` | Bearer | Deposit money (`{"amount":"10.50"}`) |
| POST | `/api/accounts/{accountNumber}/withdrawals` | Bearer | Withdraw money (`{"amount":"4.00"}`) |
| GET | `/api/accounts/{accountNumber}/balance` | Bearer | Derived balance |
| POST | `/api/transfers` | Bearer + `Idempotency-Key` | Transfer between accounts in one currency |
| GET | `/api/accounts/{accountNumber}/statements` | Bearer | Generated daily statements, newest first |
| GET | `/api/rates/usd-khr` | Bearer | Current admin-set USD/KHR rate |
| PUT | `/api/admin/usd-khr-rates` | Bearer + `ADMIN` | Publish/correct a dated rate |

A transfer looks like this:

```json
{
  "sourceAccountNumber": "9836015344",
  "destinationAccountNumber": "9004821957",
  "amount": "12.34"
}
```

The `Idempotency-Key` header is required (1–100 letters, digits, `.`, `_`, `:` or `-`).
Reusing a key with the same request returns the original response; reusing it with a
different amount or account returns `409 Conflict`.

Transfers are capped per account per business day (`Asia/Phnom_Penh`). The defaults are
$1,000.00 and 4,000,000.00 KHR, configurable in minor units through
`TRANSFER_DAILY_LIMIT_USD_MINOR` and `TRANSFER_DAILY_LIMIT_KHR_MINOR`. The check runs while
the source account row is locked, so concurrent requests cannot both slip over the limit.

A statement looks like this:

```json
{
  "accountNumber": "9836015344",
  "currency": "USD",
  "statementDate": "2026-11-18",
  "openingBalance": "0.00",
  "closingBalance": "50.00",
  "deposits": "100.00",
  "withdrawals": "30.00",
  "transfersIn": "0.00",
  "transfersOut": "20.00",
  "entryCount": 3,
  "createdAt": "2026-11-19T00:05:00Z"
}
```

The USD/KHR rate is stored as integer minor units (KHR sen per 1 USD). Rates are dated:
the current rate is the newest row effective today or earlier. Admins publish with
`PUT /api/admin/usd-khr-rates`; normal users receive `403` in the standard JSON error shape.

Amounts are decimal strings with at most two decimal places. They are stored as integer
minor units and returned as strings, so no floating-point value ever touches money.

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
2. **Balances are derived from the ledger, not stored.** `accounts` has no `balance` column. Every deposit and withdrawal appends one immutable `ledger_entries` row, and the balance is `SUM(amount_minor)` for that account. A stored counter can silently drift; history cannot.
3. **Phone number is the username.** Matches how Wing/ABA-style wallets work in Cambodia, where the phone number is the familiar login identifier.
4. **Stateless JWT auth.** Each request carries a signed token; no server-side session store. BCrypt (with per-password salt) protects the password hashes.
5. **One account per user per currency** — enforced by a DB unique constraint, not just application code. Two layers, because races happen.
6. **Flyway migrations** are Java's version of Laravel migrations: every schema change is a numbered, committed SQL file (`db/migration/V1__init.sql`).
7. **Consistent error contract.** A single `@RestControllerAdvice` translates exceptions into one predictable JSON shape; clients (and tests) can rely on it.
8. **Integration tests hit a real database.** H2 would hide Postgres behavior (constraints, identity columns, locking). Testcontainers gives production parity.
9. **Withdrawing more than the balance returns 422, not 400.** The request is well-formed and the account exists; the current state forbids the operation. No ledger row is written on an overdraft — the check happens before the insert.
10. **Ledger rows use a signed amount plus an `EntryType` label.** The sign is the money fact (`+1050`, `-400`) and the type documents the movement (`DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN`, `TRANSFER_OUT`). A database `CHECK` keeps the two from contradicting each other.
11. **Transfers are double-entry.** Each transfer writes exactly two `ledger_entries` rows — `TRANSFER_OUT` on the source (`-amount`) and `TRANSFER_IN` on the destination (`+amount`) — both linked to the same `transactions` row. Their sum is zero, so `SUM(amount_minor)` across the wallet is conserved. A `CHECK` requires transfer entries to have a transaction and deposit/withdrawal entries not to. Deposits and withdrawals are external cash flows (the counterparty is outside the wallet), so they stay single-sided.
12. **Idempotency is a database fact, not a client courtesy.** `transactions` has a `UNIQUE (user_id, idempotency_key)` constraint. A repeated key returns the stored transaction instead of moving money again. `pg_advisory_xact_lock` serializes two in-flight requests with the same key, so the second sees the first result rather than racing the unique constraint. Reusing a key for a different amount or account is a `409 Conflict`.
13. **Pessimistic locks, ordered by account id.** Every deposit, withdrawal and transfer runs `SELECT … FOR UPDATE` on its account rows before reading a balance. This is what stops two concurrent withdrawals from both reading the same balance and overdrawing. Transfers always lock the lower account id first, so A→B and B→A cannot deadlock.
14. **The audit log is append-only at the database level.** The repository exposes only `save`, and Postgres triggers reject `UPDATE`, `DELETE` and `TRUNCATE` on `audit_log`. Audit rows are written in the same transaction as the money movement: if the audit insert fails, the money movement rolls back.
15. **UTC everywhere.** Timestamps are stored as `TIMESTAMPTZ` and serialized as ISO-8601 UTC instants.
16. **Daily limits are enforced inside the account lock.** The limit check sums today's `TRANSFER_OUT` rows for the source account after acquiring its `FOR UPDATE` lock. Two concurrent transfers therefore see each other's committed effect; neither can slip past the cap. "Today" is the business day in `Asia/Phnom_Penh`, not the server's default zone.
17. **Statements are derived snapshots, not new truth.** Opening balance is the ledger sum before the day, closing is opening plus that day's signed entries. `UNIQUE (account_id, statement_date)` makes the nightly job idempotent; the job skips accounts created after the day and statements that already exist.
18. **Exchange rates are integers with dates.** The rate is stored as KHR sen per 1 USD (`410000` = 4,100.00 KHR), so rate maths never touches floating point. The current rate is the newest row with `effective_from <= today`; future rates do not apply early.
19. **Admin is a role, not a separate login system.** `users.role` is `USER` or `ADMIN`, authorities are loaded on every request, and `/api/admin/**` requires `ROLE_ADMIN`. A custom access-denied handler keeps `403` responses in the same JSON shape as the other errors.
20. **API docs are generated from the running code.** springdoc reads the controllers and DTOs, so Swagger cannot drift from the implementation. The docs are public (they expose no data), the bearer scheme is declared once, and the API itself still requires a JWT.
21. **Unit tests guard the money rules; integration tests guard the wiring.** Fast tests prove `Money`/sign/limit logic without Docker. Testcontainers tests prove the real security chain, Flyway schema, SQL constraints and row locks. Neither replaces the other.

## Project layout

```
src/main/java/com/wallet/api/
├── auth/        register + login (DTOs, service, controller)
├── account/     create/list accounts
├── ledger/      deposits, withdrawals, derived balance
├── transfer/    transfers, idempotency, daily limits, double-entry transactions
├── statement/   nightly end-of-day statements
├── rate/        admin-set USD/KHR rate
├── audit/       append-only who-did-what log
├── config/      OpenAPI/Swagger metadata
├── security/    JWT service, filter, roles, Spring Security config
├── user/        User entity + repository, /api/me
└── common/      Currency, Money, shared error handling
src/main/resources/
├── application.yml
└── db/migration/     V1–V4 Flyway schema migrations
src/test/java/com/wallet/api/
├── AbstractIntegrationTest.java   real Postgres + MockMvc base class
├── auth/AuthControllerIT.java
├── account/AccountControllerIT.java
├── ledger/                       deposit, withdraw, balance + repository IT
├── transfer/                     transfer, idempotency, limit and concurrency ITs
├── statement/                    statement service, job and controller ITs
├── rate/ExchangeRateIT.java      admin publish + user read rules
├── audit/AuditLogIT.java
├── config/OpenApiDocsIT.java     public spec + Swagger UI
├── common/MoneyTest.java
```

## Configuration

| Env var | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5434/wallet` | host port 5434 avoids clashing with a local Postgres |
| `DB_USER` / `DB_PASSWORD` | `wallet` / `wallet` | dev only |
| `JWT_SECRET` | dev-only default | must be ≥ 32 bytes; **override in any real environment** |
| `JWT_EXPIRATION_MINUTES` | `60` | |
| `SERVER_PORT` | `8080` | |
| `APP_ZONE` | `Asia/Phnom_Penh` | Business day used by limits and statements |
| `TRANSFER_DAILY_LIMIT_USD_MINOR` | `100000` | $1,000.00 per account per day |
| `TRANSFER_DAILY_LIMIT_KHR_MINOR` | `400000000` | 4,000,000.00 KHR per account per day |
| `STATEMENT_CRON` | `0 5 0 * * *` | Nightly statement run, `Asia/Phnom_Penh` |
