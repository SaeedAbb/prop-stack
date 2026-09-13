---
description: Stop the whole PropStack app - frontend, backend, and Docker infra (data volumes preserved)
---

Stop the whole PropStack application. Do this yourself with tool calls (Bash/PowerShell) - do not ask the user to run commands manually.

1. **Backend**: find `java.exe` processes whose command line contains `PropstackBackendApplication`, or contains `spring-boot:run` together with the repo's `backend` path (e.g. via `Get-CimInstance Win32_Process` filtered on `Name='java.exe'`). Stop each one (`Stop-Process -Force`). Note: on Windows/Git Bash, a backgrounded shell job's PID does not reliably correspond to the real `java.exe` process, so always match by command line / `Get-CimInstance`, not by a remembered job PID.

2. **Frontend**: find `node.exe` processes whose command line matches `ng.js` and `serve`. For each, stop it and also stop its parent process (the `npm start` wrapper), since killing only the child can leave an orphaned `npm` process behind.

3. **Infra**: from the repo root, run `docker compose stop` (not `down -v` - preserve the Postgres/Keycloak data volumes; only wipe them if the user explicitly asks for a clean slate).

4. Report concisely what was actually stopped vs. already not running.
