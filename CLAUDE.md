# CLAUDE.md

Guidance for Claude Code (and any other AI agent) working in this repository. For how to actually
run the app, see `README.md` (backend/frontend/infra startup) and `keycloak/README.md` (realm,
organizations, membership feature, production deltas) — this file does not repeat those steps.

## Project Overview

PropStack is a property management app: Spring Boot backend (`backend/`), Angular frontend
(`frontend/`), Keycloak-based auth/multi-tenancy (`keycloak/`), all wired together by the
repo-root `docker-compose.yml` (Postgres, Keycloak, Keycloak's own Postgres, Mailpit).

## Architecture Notes

### Backend Architecture (package-by-feature, split by sub-concept, then layered)

Top-level packages are bounded contexts (`com.propstack.property`, `com.propstack.organization`),
plus flat, single-purpose packages for cross-cutting infra (`config`, `security`) that have
nothing to layer or split further.

**Within a bounded context, split into one sub-package per distinct concept it owns, before any
technical layering.** Don't let two different concepts' classes sit side by side in the same
`persistence/`/`dto/` folder just because they belong to the same top-level feature — that's the
mistake this convention exists to prevent (it's what `Address` living inside `property`'s own
`persistence/`/`dto/` folders used to look like). Current example split:

- `property.property` — the `Property` aggregate itself.
- `property.address` — the `Address` concept, which `Property` owns (`@OneToOne`) but which is
  its own thing (own table, own request/response shape, own mapper). No `web/` here — `Address`
  isn't independently exposed via REST, it's always accessed through `Property`.
- `organization.organization` — the organization-membership domain: resolving the caller's
  organization from their JWT, listing/adding/removing members, the domain-facing
  controller/service/DTOs/exceptions. Knows it needs Keycloak, but not the wire-level details.
- `organization.keycloak` — the Keycloak Admin API integration: the REST client, its
  `@ConfigurationProperties`, the service-account token provider, and the raw wire-format records
  Keycloak returns (`KeycloakOrganizationMember`, `KeycloakUserSummary`, etc.). No `web/`, no
  `dto/` of its own — this whole sub-package *is* the "external client" role; it's consumed
  directly by `organization.organization`'s service layer.

**Within each concept sub-package, layer by technical role** (only the layers that concept
actually needs):
- `web/` — controllers (REST endpoints only, no business logic). Omit if the concept has no
  independent endpoints (e.g. `address`, `keycloak`).
- `service/` — domain services + MapStruct mappers (entity ↔ DTO conversion, business logic).
- `persistence/` — JPA entities (aggregate roots), enums (`@Enumerated(EnumType.STRING)`), Spring
  Data repositories. A concept whose system of record is an external service instead of a
  database (e.g. `organization.keycloak`) has no `persistence/` — its client/config/wire-DTO
  classes sit flat at the sub-package root instead.
  - `dto/` — request/response DTOs; the *only* types a controller may accept or return.
  - `exception/` — concept-specific exceptions.
- A concept's mapper composes a sibling concept's mapper for nested objects via MapStruct's
  `uses = ...` (e.g. `PropertyMapper` is `@Mapper(uses = AddressMapper.class)` so `Property`'s
  nested `Address` field maps/updates-in-place through `AddressMapper`, not inline).

When adding a genuinely new concept to an existing bounded context, give it its own sub-package
from the start rather than bolting it onto an existing one's layers.
- **Entities never leave the service layer** — controllers only ever see DTOs.
- **MapStruct** (`@Mapper(componentModel = "spring")`) for all entity↔DTO conversion.
- **Exception handling is per-exception, not global**: no `@ControllerAdvice`. A feature exception
  carries its own `@ResponseStatus` (e.g. `PropertyNotFoundException` →
  `@ResponseStatus(HttpStatus.NOT_FOUND)`). Keep following this pattern rather than introducing a
  global exception handler.
- **Fixed value sets are Java enums** (`riskLevel`/`status`/`type`-shaped fields), mapped via
  `@Enumerated(EnumType.STRING)`.
- **Surrogate keys only**: `@Id` is a UUID or auto-increment `Long`/IDENTITY. Any externally
  sourced natural key is a separate indexed/unique column, never the primary key.
- **Lombok** (`@Getter`/`@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`/`@Builder`) for
  entities and DTOs instead of hand-rolled boilerplate.
- REST endpoints return `List<X>`/`X` directly for plain GETs; `ResponseEntity<>` only when a
  custom status/header is actually needed (201 + `Location` on create, etc.).
- Database migrations are Flyway-versioned (see Database Management below).

### Domain-Driven Design Principles

- **Ubiquitous language**: business terms in code (`Property`, `Organization`, not
  `PropertyEntity`/`OrgDTO`-style noise).
- **Aggregate boundaries**: each top-level package is a bounded context, and each concept
  sub-package within it (e.g. `property.address`, `organization.keycloak`) is its own aggregate —
  don't reach into another concept's `persistence/` (or client) package directly, go through its
  `service/` layer (mapper composition via MapStruct `uses = ...` is the one exception, since that
  is itself the concept's own mapping service).
- **Minimal coupling**: cross-concept and cross-context communication goes through a concept's own
  service/DTOs.
- **Domain focus**: business logic lives in domain entities and services, not controllers.
- **Infrastructure separation**: technical concerns (`config`, `security`, HTTP clients) stay out
  of feature packages.
- **Naming consistency across the stack**: one canonical spelling per concept across
  entity/DTO/frontend model — grep for the existing name before introducing a new one for the
  same thing.

### Frontend Architecture

- **Feature-based organization**: `core/` (config, constants, guards, models, services shared
  across features) + `features/<feature>/<component>` (e.g. `features/properties/property-list`)
  + `shared/pipes/` (derived-value pipes reused across features).
- **Standalone components** — the Angular 19+ default; never set `standalone: true` explicitly.
- **`inject()`**, not constructor injection.
- **Signals** (`signal`/`computed`/`effect`) and `toSignal()` for derived/async state instead of
  manual RxJS subscriptions; use the async pipe where a template still needs an `Observable`
  directly. Manage any remaining subscriptions explicitly (`takeUntilDestroyed`) to avoid leaks.
- **New control-flow syntax**: `@if`/`@for`, not `*ngIf`/`*ngFor`.
- **Angular Material** (`@angular/material` + `@angular/cdk`) is the UI component library.
- **Strict typing everywhere** — avoid `any`; type HTTP `subscribe`/`pipe` callbacks explicitly
  (`next: (data: X[]) => ...`, `error: (err: HttpErrorResponse) => ...`).
- **Derived-value getters become pipes** (`shared/pipes/`), not plain methods called from
  templates — better change-detection performance.
- Date fields modeled as `Date` objects (not strings), formatted in templates via Angular's
  `DatePipe` rather than manual parsing/formatting.
- No inline `style="..."` in templates — styling lives in the component's `.scss`.
- No `!important` — rely on selector specificity/ordering.
- Hardcoded lookup maps/constants live in `core/constants/`, not inline in components.
- Avoid px units except `max-width`, base `font-size`, and border sizes — prefer rem/em/%.

## Security Features

- **Keycloak-backed OAuth2 resource server**: the backend validates JWTs against
  `spring.security.oauth2.resourceserver.jwt.issuer-uri` (Keycloak `PropStack` realm); realm roles
  are mapped via `KeycloakRealmRoleConverter` (`security/`).
- **Organization-scoped multi-tenancy**: every access token carries an `organization` claim;
  `CurrentOrganizationResolver` (`organization.organization.service`) resolves it so a user only
  ever sees their own organization's data (e.g. properties). Don't add an endpoint that queries
  across organizations without going through this resolver.
- **`org-admin` realm role** gates organization-membership management
  (`organization.organization.web.OrganizationMemberController`), which calls Keycloak's Admin
  REST API via a service-account client (`organization.keycloak.KeycloakAdminClient`) — see
  `keycloak/README.md` for the full membership design.
- **Input validation** via Bean Validation (`spring-boot-starter-validation`) on request DTOs.
- **CORS** centralized in `CorsConfig`/`CorsProperties`, allowed origin(s) sourced from
  `app.cors.allowed-origins` (env/property-driven) — never hardcode an origin.
- **Dependency scanning**: Trivy filesystem scan runs in CI (`security-scan` job), results
  uploaded to GitHub code scanning.
- **Known gaps — don't assume these are handled**:
  - Neither `backend/Dockerfile` nor `frontend/Dockerfile` sets a non-root `USER` yet.
  - `frontend/nginx.conf` sets no security headers (CSP, HSTS, etc.) yet.
  - The Keycloak `propstack-backend` client secret is a committed dev-only placeholder
    (`dev-only-propstack-backend-secret`) — must be rotated via a real secrets manager before any
    non-local deployment (see `keycloak/README.md` → Production deltas).

## API Documentation

No OpenAPI/Swagger is configured (no `springdoc-openapi` dependency, no `@Operation`/`@Schema`
annotations anywhere) — don't reference a Swagger UI URL as if it exists. If this is added later,
the standard approach is `springdoc-openapi-starter-webmvc-ui`, exposing `/swagger-ui.html` and
`/v3/api-docs`.

## Database Management

- Flyway migrations: `backend/src/main/resources/db/migration/`, naming
  `V{version}__{description}.sql` (e.g. `V1__create_property_table.sql`).
- `spring.jpa.hibernate.ddl-auto=validate` — Flyway owns the schema, Hibernate only validates
  entity mappings against it. Every schema change is a new migration, never a Hibernate
  auto-DDL change.
- Dev database: Postgres via `docker compose up -d` (`localhost:5432`, db/user/pass `propstack`).
- **Test gap**: there is no dedicated test datasource (no H2, no Testcontainers) — service/mapper/
  controller tests use Mockito/`@WebMvcTest` slices (no real DB involved), but
  `PropstackBackendApplicationTests` is a full `@SpringBootTest` context-load check that connects
  to the **real local dev Postgres** (`localhost:5432`) and runs Flyway against it. That means
  `./mvnw test` only fully passes with `docker compose up -d` already running (see Troubleshooting
  → Health Checks). Given Flyway runs Postgres-specific SQL, Testcontainers (real Postgres) is the
  recommended upgrade over depending on the live dev database, whenever backend test coverage
  grows further.

## Environment Configuration

### Local Development

| Service | URL/Port |
|---|---|
| Frontend (`ng serve`) | http://localhost:4200 |
| Backend API | http://localhost:8080/api |
| Postgres (app DB) | localhost:5432 |
| Keycloak | http://localhost:8081 (admin console + realm), management/health on :9000 |
| Keycloak's own Postgres | localhost:5433 |
| Mailpit (dev SMTP catcher) | http://localhost:8025 (web UI), :1025 (SMTP) |

No PgAdmin service and no Compose `profiles:` — `docker compose up -d` always starts all four
infra services together.

### Docker Images

- `backend/Dockerfile`: multi-stage (`eclipse-temurin:26-jdk` build → `eclipse-temurin:26-jre`
  runtime), exposes 8080.
- `frontend/Dockerfile`: multi-stage (`node:22-alpine` build → `nginx:1.27-alpine` runtime),
  serves the built SPA on port 80 with a simple `try_files` fallback (`nginx.conf`) — no API
  proxying configured there.

## Deployment

### CI/CD Pipeline (`.github/workflows/ci-cd.yml`)

Runs on push/PR to `main`:
- `backend-test` — Postgres 16 service container, JDK 26 (temurin), `./mvnw -B clean test`,
  publishes a JUnit report.
- `frontend-test` — Node 22, `npm ci`, `npm run lint`, `npm run test:ci`, `npm run build`,
  uploads `frontend/dist/frontend/browser/` as an artifact.
- `security-scan` — Trivy filesystem scan → SARIF → GitHub code scanning tab.
- `build-images` — **only** on a push to `main`, and only after `backend-test`/`frontend-test`
  pass: builds and pushes both `backend/Dockerfile` and `frontend/Dockerfile` images to
  `ghcr.io/<repo>/{backend,frontend}` via Buildx, with GHA layer caching.

### Production Deployment

- Flyway migrations run automatically on backend startup — not a separate manual step.
- Deploy the `ghcr.io` images (docker-compose or an orchestrator of your choice).
- The repo's `docker-compose.yml` is dev/local-only; see `keycloak/README.md` → "Production
  deltas" for what changes for Keycloak specifically (hostname/TLS, secrets manager, HA, etc.) —
  don't re-derive this, that section is the source of truth.
- Verify readiness the same way the repo's own `/start` skill does (no actuator is configured):
  poll `GET /api/properties` and treat `200`/`401`/`403` as "up."

## Common Tasks

### Adding a New Backend Bounded Context

Follow the concept-then-layer package pattern (see Architecture Notes):

```
com/propstack/<context>/<concept>/
├── web/
│   └── <Concept>Controller.java        # REST endpoints only — omit if not independently exposed
├── service/
│   ├── <Concept>Service.java           # business logic
│   └── <Concept>Mapper.java            # MapStruct entity <-> DTO; uses = <OtherConcept>Mapper.class
│                                        # for any nested object owned by a sibling concept
├── persistence/                        # entity/enum/repository — or omit entirely if this
│   ├── <Concept>.java                  # concept's system of record is an external service
│   ├── <Concept>Status.java            # (client/config/wire-DTO classes sit flat at the
│   └── <Concept>Repository.java        # sub-package root instead, e.g. organization.keycloak)
├── dto/
│   ├── <Concept>Request.java
│   └── <Concept>Response.java
└── exception/
    └── <Concept>NotFoundException.java # @ResponseStatus, not a global handler
```

1. Decide if this is a new concept inside an existing bounded context (new sub-package under
   `property`/`organization`) or a whole new bounded context (new top-level package).
2. Create the Flyway migration (`V{next}__description.sql`) if it owns a table.
3. Implement entity/enum, repository, MapStruct mapper, service, controller, DTOs, exceptions —
   only the layers this concept actually needs.
4. Use business terminology throughout; keep concepts loosely coupled — a sibling concept's
   internals are reached through its service/mapper, not by poking its persistence layer directly.
5. Add tests for the new service/controller (see Database Management's test gap note).

### Extending an Existing Concept

1. Add to the appropriate concept sub-package's existing layers (e.g. a new endpoint goes in that
   concept's `web/`, not a different concept's).
2. Add a Flyway migration if the schema changes.
3. Update repository/service/controller/DTOs as needed.
4. Add/extend tests.

### Adding a New Frontend Feature

1. `ng generate component features/<feature-name>/<component-name>`.
2. Add an API service under `core/services/` (`inject(HttpClient)`, strictly typed responses).
3. Add routing in `app.routes.ts` if it's a new route.
4. Build the UI with Angular Material components, `@if`/`@for`, signals for derived state.
5. Extract any derived-value display logic into a pipe under `shared/pipes/`.
6. Add unit tests (Vitest, via `ng test` / `npm run test:ci` — not Jasmine/Karma).

### Database Schema Changes

1. New Flyway migration: `V{next_version}__{description}.sql`.
2. Update the JPA entity/enum to match.
3. Test locally — `docker compose down -v && docker compose up -d` for a clean re-run of all
   migrations from scratch, or just restart the backend against the existing volume for an
   incremental one.
4. Update tests/docs.

## Troubleshooting

### Common Issues

- **Port conflicts**: 8080 (backend), 4200 (frontend dev server), 5432 (app Postgres), 5433
  (Keycloak's Postgres), 8081 (Keycloak), 9000 (Keycloak management), 8025/1025 (Mailpit).
- **Docker issues**: `docker compose down` (keeps named volumes/data) then `docker compose up -d`.
  Only use `docker compose down -v` when you deliberately want to wipe Postgres/Keycloak data.
- **Keycloak realm changes not taking effect**: `--import-realm` only imports
  `PropStack-realm.json` on a **fresh** volume (first boot). Editing that file later does nothing
  until you `docker compose down -v` (destructive — drops all Keycloak data) or make the
  equivalent change manually via the Admin Console.
- **Frontend `npm install` peer-dependency errors**: use `npm install --legacy-peer-deps` (known
  npm/arborist issue with the current Angular 22 toolchain, see `README.md`).
- **`./mvnw test` fails with a connection error to Postgres**: `PropstackBackendApplicationTests`
  boots the real Spring context against `localhost:5432` — run `docker compose up -d` first (see
  Database Management's test gap note).
- **Angular build errors**: clear `node_modules` and reinstall (`--legacy-peer-deps`).

### Health Checks

- Backend: no `/actuator/health` (actuator isn't a dependency) — poll `GET /api/properties`
  instead; `200`/`401`/`403` all mean the app is up and responding.
- Database: check backend startup logs for Flyway migration output.
- Keycloak: its own container healthcheck hits `/health/ready` on the management port (9000).
- Frontend: check the `ng serve` terminal output / browser console.

## Best Practices

### Mandatory Pre-Commit Checklist

Before every commit and push:

1. **Frontend lint**: `cd frontend && npm run lint` — fix all errors/warnings, don't commit with
   lint issues.
2. **Frontend tests**: `cd frontend && npm run test:ci` — all tests must pass.
3. **Backend tests** (if backend changed): `cd backend && ./mvnw test` — all tests must pass.
   (There is no backend lint tool configured — don't invent a Checkstyle/Spotless step that
   doesn't exist.)
4. **Review the diff**: `git diff --staged` — only intended changes, no debug `console.log`/debug
   code, no secrets.

These checks run in CI too (`backend-test`, `frontend-test` jobs) — running them locally first
avoids pipeline failures.

### Code Quality

- Follow the established backend/frontend conventions above consistently across every feature,
  not just the first one.
- Meaningful commit messages; keep methods small and focused.
- Comments only where the *why* isn't obvious from the code itself (a non-obvious constraint, a
  workaround, a subtle invariant) — not restating what the code already says.

### Security

- Never commit secrets or real API keys/credentials.
- Validate all user input at the boundary (Bean Validation on request DTOs).
- Rely on JPA/parameterized queries — never hand-built SQL string concatenation.
- Rotate the dev-only Keycloak client secret (and any other dev default) before a real deployment.
- Keep dependencies updated; Trivy in CI will flag known vulnerabilities.

### Performance

- Index FK/lookup columns in migrations (already done for `property.organization_id`).
- Consider `ChangeDetectionStrategy.OnPush` as components grow — not used anywhere yet, so this is
  a forward-looking recommendation, not an existing pattern to copy.
- Paginate list endpoints once data volume warrants it (not implemented yet for
  `GET /api/properties`).
- Both Dockerfiles already use multi-stage builds to keep runtime images slim.