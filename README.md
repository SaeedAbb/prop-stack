# PropStack

Property management application.

- `backend/` — Spring Boot 4.1.1 (Java 26, Maven), Flyway-managed schema
- `frontend/` — Angular 22 (standalone components, SCSS)
- `keycloak/` — realm import assets for the Keycloak identity server
- `docker-compose.yml` (repo root) — all infrastructure: the app's Postgres, Keycloak's Postgres, Keycloak itself, and Mailpit (dev SMTP catcher)

## Infrastructure (Postgres + Keycloak + Mailpit)

Requires Docker Desktop running. One command brings up everything the app needs:

```
docker compose up -d
```

This starts:
- `postgres` — the app's own database (`localhost:5432`, db/user/pass `propstack`), schema owned by Flyway
- `keycloak-postgres` — Keycloak's own database (`localhost:5433`)
- `keycloak` — Keycloak itself (`http://localhost:8081`), with the `PropStack` realm, its
  `propstack-frontend` and `propstack-backend` clients, and example seed data (organization
  `example-org`, users `example-member` and `example-admin`) imported automatically on first boot
- `mailpit` — catches emails Keycloak sends (e.g. organization invites) — view them at `http://localhost:8025`

Keycloak Admin Console: `http://localhost:8081` — bootstrap admin `admin` / `admin` (dev only).
See `keycloak/README.md` for the realm details and the organization-admin membership feature.

Any future infra service (cache, etc.) belongs in this same `docker-compose.yml`.

## Backend

Requires the infrastructure above running first (connects to `postgres`, runs Flyway migrations,
validates JWTs against Keycloak, and calls Keycloak's Admin REST API for organization membership).

```
cd backend
./mvnw spring-boot:run
```

API available at `http://localhost:8080/api`:
- `GET/POST/PUT/DELETE /api/properties` — organization-scoped (a user only ever sees their own
  organization's properties); requires a valid Keycloak-issued bearer token
- `GET/POST /api/organization/members`, `DELETE /api/organization/members/{userId}` — list/add/remove
  members of the caller's own organization; requires the `org-admin` realm role

## Frontend

Requires the infrastructure above running first (redirects to Keycloak for login).

```
cd frontend
npm install --legacy-peer-deps
npm start
```

App available at `http://localhost:4200`, calling the backend at `http://localhost:8080/api`
(see `src/environments/environment.ts`). Users holding the `org-admin` realm role see an
**Admin** link in the nav, leading to `/admin/members` — add/remove their organization's members.

> Note: `npm install` currently needs `--legacy-peer-deps` due to an npm/arborist bug
> with the newest Angular 22 toolchain's peer dependency graph.
