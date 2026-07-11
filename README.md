# Rhea

Coverage pairing for unemployed Coloradans: verified unemployment status becomes the
eligibility signal for an AI-assisted application to a real state program, with the
pharmacy appealing the original bill once that application is approved.

Built under the Callisto brand system (Callisto Consulting Group LLC DBA Callisto Tech).

## What it does

1. **Verify** -- the pharmacy confirms a patient's active Colorado unemployment claim.
2. **Dispense** -- once verified, the pharmacist dispenses the prescription. The patient is
   billed at retail price by default -- there is no automatic $0. The bill only clears once
   an approved application is appealed and paid (steps 3-4).
3. **AI coverage pairing** -- an AI agent (Groq / `openai/gpt-oss-120b` via Spring AI,
   tool-calling into Tavily live search) drafts a real, currently-available Colorado public
   program application for the patient. Drafting never enrolls anyone: the application sits
   at `DRAFTED` until the patient explicitly consents. Agreeing moves it to `SUBMITTED`;
   declining moves it to `DECLINED` and nothing further happens. There is no live state API
   to poll, so pharmacy staff record the state's decision manually (`APPROVED`/`DENIED`),
   the same pattern already used for the CDLE manual override.
4. **Pharmacy appeal** -- once the application is `APPROVED`, the pharmacy appeals the
   original bill from step 2 against that program. Marking the appeal `PAID` clears the
   prescription's bill status.

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

## Auth

DB-backed login (`users` table, `id` / `user_name` / `name` / `password_hash` / `provider` /
`active` / `date_created` -- see `V5__users_table.sql`), with two ways in:

- **Standalone account creation** -- `/login` has a "Create Account" tab that posts to
  `/api/auth/register` (name, email, password >= 8 chars), bcrypt-hashed via Spring
  Security's `DaoAuthenticationProvider`.
- **Google OAuth2** -- "Continue with Google" at `/oauth2/authorization/google`. First
  sign-in auto-creates a `users` row (`provider='google'`, no password hash). Requires
  `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` env vars from a project at
  https://console.cloud.google.com/apis/credentials -- unset locally, the app still boots
  (placeholder client id/secret), the Google button just won't complete a real login until
  real credentials are provisioned.

Deactivating a user (`active=false`) blocks that account from signing in without deleting
the row.

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
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 login | Recommended for a shared deploy |
| `GROQ_API_KEY` | Powers the AI insurance-match agent | Yes, for AI matching to work |
| `TAVILY_API_KEY` | Live search for the AI agent's tool calls | Yes, for live search to work |
| `DATABASE_URL` / `DATASOURCE_*` | Only if moving off H2 | No |

Without `GROQ_API_KEY` / `TAVILY_API_KEY`, the app still runs -- the AI pairing endpoint
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
