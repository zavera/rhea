# Rhea

$0 prescriptions for unemployed Coloradans, verified and reimbursed automatically.

Built under the Callisto brand system (Callisto Consulting Group LLC DBA Callisto Tech).

## What it does

1. **Verify** -- the pharmacy confirms a patient's active Colorado unemployment claim.
2. **Dispense at $0** -- once verified, the pharmacist dispenses the prescription for $0.
   Rhea records the dispense and immediately files a reimbursement claim against a state
   program (default: Colorado Indigent Care Program) on the pharmacy's behalf.
3. **AI insurance match** -- an AI agent (Groq / `openai/gpt-oss-120b` via Spring AI,
   tool-calling into Tavily live search) finds the patient a real, currently-available
   Colorado public insurance program to enroll in for ongoing coverage.

## Database decision

Ships with **H2, file-backed** (`./data/rhea`) for zero-setup local runs and single-instance
deploys -- no external database to provision. Flyway migrations live in
`src/main/resources/db/migration/`. Tests use H2 in-memory.

To move to a shared/production Postgres instance, override these four properties via env
vars (Railway's Postgres plugin injects a compatible `DATABASE_URL` automatically):

```
DATABASE_URL=jdbc:postgresql://...
DATASOURCE_DRIVER=org.postgresql.Driver
DATASOURCE_USERNAME=...
DATASOURCE_PASSWORD=...
```

and add the `org.postgresql:postgresql` runtime dependency to `pom.xml`.

## Auth (MVP)

Bcrypt-backed local login for pharmacy staff (`PHARMACY_USER` / `PHARMACY_PASSWORD` env
vars, defaults to `pharmacist` / `changeme-local-dev` for local dev only -- **override both
before any shared deploy**). Google OAuth2 (per the Callisto brand system) is a follow-up
once client credentials are provisioned.

`/api/**` is authenticated by default (`REQUIRE_AUTH=true`). Set `REQUIRE_AUTH=false` to
open the API up without a login -- this is used by the local demo launch config
(`.claude/launch.json`, `rhea` entry) so the demo runs login-free without deleting the auth
setup. `/login` keeps working either way. **Never set `REQUIRE_AUTH=false` on a shared or
production deploy** -- flip it back to `true` (or leave it unset) before a real client uses
this.

The unemployment verification service (`CdleStubVerificationService`) is a stand-in for a
real Colorado Department of Labor and Employment (CDLE) claimant lookup, which has no
public API today. It treats claim numbers matching `CO-YYYY-NNNNNN` as verified. Swap in a
real CDLE integration by adding another `UnemploymentVerificationService` implementation.

## Required environment variables

| Variable | Purpose | Required |
|---|---|---|
| `PORT` | HTTP port (Railway injects this) | No, defaults to 8080 |
| `PHARMACY_USER` / `PHARMACY_PASSWORD` | Pharmacy staff login | Recommended to override |
| `GROQ_API_KEY` | Powers the AI insurance-match agent | Yes, for AI matching to work |
| `TAVILY_API_KEY` | Live search for the AI agent's tool calls | Yes, for live search to work |
| `DATABASE_URL` / `DATASOURCE_*` | Only if moving off H2 | No |

Without `GROQ_API_KEY` / `TAVILY_API_KEY`, the app still runs -- the AI match endpoint
returns a graceful fallback using the curated program list instead of a live answer.

## Run locally

```
mvn spring-boot:run
```

or build and run the jar directly:

```
mvn -DskipTests package
PORT=8080 java -jar target/rhea-0.1.0.jar
```

Visit `http://localhost:8080`. Log in as pharmacy staff via the nav's "Pharmacy staff
login" link, then walk through the demo panel at the bottom of the page.

## Testing

```
mvn verify
```

Runs unit tests, then integration tests (`*IT.java`) against H2. The AI layer is mocked
in tests (`ChatModel` bean) -- no real LLM calls happen in CI.

## Deployment

Deploys to Railway via `railway.toml` (nixpacks build, `/actuator/health` healthcheck).
Set all required environment variables in the Railway dashboard -- never commit secrets.
