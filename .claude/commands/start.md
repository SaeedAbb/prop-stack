---
description: Start the whole PropStack app - Docker infra (Postgres, Keycloak, Mailpit), backend, and frontend
---

Start the whole PropStack application. Do this yourself with tool calls (Bash/PowerShell) - do not ask the user to run commands manually.

1. **Infra**: from the repo root, run `docker compose up -d`. Then poll `docker inspect -f '{{.State.Health.Status}}' propstack-keycloak-1` every few seconds (cap at ~2 minutes) until it reports `healthy`.

2. **Backend**: check first whether it's already running - look for a `java.exe` process whose command line contains `PropstackBackendApplication` or (`spring-boot:run` and the repo's `backend` path). If not running, start it in the background from `backend/`:
   `JAVA_HOME="C:\Users\SAbbas\.jdks\openjdk-26.0.2.1" ./mvnw.cmd -q spring-boot:run` (redirect output to a log file, e.g. in the scratchpad directory). Poll `http://localhost:8080/api/properties` until it returns 200/401/403 (cap at ~2 minutes).

3. **Frontend**: check first whether it's already running - look for a `node.exe` process whose command line matches `ng.js` and `serve`. If not running, start it in the background from `frontend/`: `npm start` (redirect output to a log file). Poll `http://localhost:4200` until it returns 200 (cap at ~2 minutes).

4. Report the result concisely: what was already running vs. newly started, and the four URLs (frontend `http://localhost:4200`, backend `http://localhost:8080/api`, Keycloak `http://localhost:8081`, Mailpit `http://localhost:8025`). If anything failed to become healthy/ready in time, say so plainly and point at the relevant log file instead of guessing why.
