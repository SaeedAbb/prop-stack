# Keycloak (PropStack realm)

Keycloak, its own Postgres, the app's own Postgres, and Mailpit (a dev SMTP catcher) are all
started together from the repo-root `docker-compose.yml`:

```
docker compose up -d
```

- Admin Console: http://localhost:8081 — bootstrap admin `admin` / `admin` (dev only, set via
  `KC_BOOTSTRAP_ADMIN_USERNAME`/`KC_BOOTSTRAP_ADMIN_PASSWORD` in `docker-compose.yml`)
- Mailpit (catches emails Keycloak sends, e.g. org invites): http://localhost:8025
- On first boot, Keycloak imports `keycloak/realm-export/PropStack-realm.json` automatically
  (via `--import-realm`), which creates:
  - The `PropStack` realm, with the **Organizations** feature enabled
    (`organizationsEnabled: true`), **Fine-Grained Admin Permissions** enabled
    (`adminPermissionsEnabled: true`), and an `smtpServer` block pointed at Mailpit
  - A public SPA client, `propstack-frontend` (PKCE, no client secret) — its `defaultClientScopes`
    includes `organization`, so every access token carries an `organization` claim
    (`["<org-alias>"]`) identifying which organization the signed-in user belongs to
  - A confidential service-account client, `propstack-backend` (secret `dev-only-...`, see
    below) — used by the Spring Boot backend to call Keycloak's Admin REST API
  - The realm role `org-admin` — held by a user who administers their own organization's
    membership via PropStack's own **Admin** section in the app itself, not Keycloak's console
  - One example organization, `example-org` (domain `example-org.propstack.test`), with two
    example seed users: `example-member` (plain member) and `example-admin` (holds `org-admin`),
    both password `example-password` — throwaway seed data, delete once verified

> The file **must** be named `PropStack-realm.json` (matching the realm name's exact casing) —
> Keycloak's importer rejects a mismatch.

## Adding a real organization

Organizations and their members are still created through Keycloak's own Admin Console
(Organizations → Create organization) — that part hasn't changed. What's different from the
original plan: **day-to-day membership management (adding/removing users) now happens inside
PropStack's own Admin section** (`/admin/members` in the Angular app), not Keycloak's console —
see the next section.

## Organization-admin membership management (PropStack's own Admin section)

Any user holding the `org-admin` realm role sees an **Admin** link in the app's nav and can, from
`/admin/members`:
- List their own organization's members
- Add a member by email — if the email matches an existing Keycloak user, they're added
  immediately; otherwise Keycloak invites them as a brand-new user (an email is sent — check
  Mailpit in dev)
- Remove a member — this also force-invalidates their Keycloak session (`POST
  /users/{id}/logout`), so they're logged out promptly rather than waiting for their token to
  expire naturally

This is implemented entirely in the PropStack backend (`com.propstack.organization` package),
which calls Keycloak's Admin REST API using the `propstack-backend` service-account client's own
`client_credentials` token — **not** Keycloak's per-user Fine-Grained Admin Permissions (FGAP)
delegation. FGAP-for-Organizations exists in this Keycloak version (confirmed via
`GET /admin/realms/PropStack/clients/{admin-permissions-client-id}/authz/resource-server`, which
shows an `Organizations` resource type with `view`/`manage` scopes), but its Admin Console
"Create permission" flow did not respond reliably during testing — this app-level approach
sidesteps that entirely and is fully verified working end-to-end.

**Granting `org-admin`**: for now, done manually via the Admin Console (Users → select user →
Role mapping → assign `org-admin`) or by adding it to a user's `realmRoles` in the realm-export
JSON, as done for `example-admin`.

**Setting the `propstack-backend` client secret**: the realm-export ships a fixed dev-only secret
(`dev-only-propstack-backend-secret`), matched by the backend's own default
(`application.properties`, `keycloak.admin.client-secret`). In a real deployment, rotate this
secret via the Admin Console/Admin REST API post-import and set `KEYCLOAK_BACKEND_CLIENT_SECRET`
from a real secrets manager — never the committed file. The service account currently holds the
full `realm-admin` composite role (`realm-management` client) — a deliberately broad default for
this private, single-purpose backend; tighten to minimal fine-grained roles later once the exact
Admin API surface in use is finalized.

**Known limitation**: forced logout invalidates the removed user's session and refresh token (so
their *next* login/refresh fails), but does not retroactively invalidate an already-issued,
unexpired access token, since the backend does stateless JWT validation with no introspection.
Keep the realm's access-token lifespan short to minimize this window.

## Email invitations (SMTP)

Real invite/setup emails are caught by **Mailpit** in dev (http://localhost:8025) — the realm's
`smtpServer` block points at it. For a real deployment, replace this with real SMTP relay
credentials, injected via a secrets manager, never committed into `PropStack-realm.json`.

## Production deltas (this compose file is dev/local only)

- `start-dev` → `start --optimized` (requires a build step baking in build-time options)
- Set a real `KC_HOSTNAME`, drop `KC_HOSTNAME_STRICT=false`, and terminate TLS (direct certs, or
  `KC_PROXY_HEADERS=xforwarded` behind a reverse proxy)
- Inject secrets (admin password, DB password, `propstack-backend` client secret, SMTP
  credentials) via a secrets manager — never plain env vars in a checked-in compose file
- `--import-realm` is a first-boot convenience only; after that, manage realm state live
  (Admin Console / Admin REST API), keeping the JSON export only as an initial-seed/DR artifact
- Run multiple Keycloak replicas behind a load balancer for HA, backed by a durable, backed-up
  Postgres instance — not this dev volume
