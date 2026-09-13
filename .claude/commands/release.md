---
description: Cut an independent semver release for the backend and/or frontend, based on Conventional Commits since each app's last release tag, and open a PR
---

Do this yourself with tool calls (Bash) - do not ask the user to run commands manually. Backend and frontend version **independently**: only commits touching `backend/` affect `backend/pom.xml`'s version, only commits touching `frontend/` affect `frontend/package.json`'s version. Evaluate both; only act on the ones that actually need a release.

For **each** app (`backend` → path `backend/`, current-version command `./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout` run from `backend/`; `frontend` → path `frontend/`, current-version command `node -p "require('./package.json').version"` run from `frontend/`):

1. **Find the last release tag**: `git describe --tags --match '<app>-v*' --abbrev=0 2>/dev/null`. If none exists, treat the repo's very first commit (`git rev-list --max-parents=0 HEAD`) as the base instead - this is the first-ever release for that app.

2. **List qualifying commits since that point, scoped to the app's own path**: `git log <base>..HEAD --pretty=format:'%s%n%b---COMMIT-END---' -- <path>/`. If this is empty, this app needs no release - skip it entirely (no branch, no version bump, no PR content for it).

3. **Decide the bump size** by scanning those commit subjects/bodies:
   - **major** if any commit has a `BREAKING CHANGE:` footer in its body, or a subject matching `^(feat|fix)(\(.+\))?!:`
   - else **minor** if any commit subject matches `^feat(\(.+\))?:`
   - else **patch** if any commit subject matches `^fix(\(.+\))?:`
   - else this app needs no release either (nothing conventional-commit-shaped touched it) - skip it.
   Take the single largest bump size found across all qualifying commits for that app.

4. **Read the current version**:
   - backend: run the current-version command above from `backend/`; if it ends in `-SNAPSHOT`, strip that suffix before bumping.
   - frontend: run the current-version command above from `frontend/`.

5. **Compute the new version** by applying the bump to the current version per normal semver rules (major: `X+1.0.0`; minor: `X.Y+1.0`; patch: `X.Y.Z+1`).

6. **Write the new version using the project's own tooling** - never hand-edit `pom.xml`/`package.json`:
   - backend: `./mvnw versions:set -DnewVersion=<new> -DgenerateBackupPoms=false` (run from `backend/`)
   - frontend: `npm version <new> --no-git-tag-version` (run from `frontend/`; this also correctly updates `package-lock.json`)

If **neither** app needed a release after the steps above, report that plainly (list what you checked and why nothing qualified) and stop - do not create a branch or push anything.

Otherwise, for whichever app(s) *did* get a new version:

7. Create a branch named `release/<YYYYMMDD-HHMM>` (UTC timestamp) from the current branch.
8. Commit with a message that says exactly what changed and why, e.g.:
   ```
   chore(release): backend 1.1.0 (minor - feat), frontend unchanged
   ```
   or, if both bumped:
   ```
   chore(release): backend 1.1.0 (minor), frontend 2.0.0 (major - breaking change)
   ```
9. Push the branch (`git push -u origin <branch>`). Do **not** attempt to use the `gh` CLI - it is not installed in this environment (confirmed). `git push` on a new branch already prints a "Create a pull request for '<branch>' on GitHub by visiting: ..." URL - surface that exact link to the user as the next step, along with a short summary of which app(s) bumped, to what version, and why (the bump size and the commits that drove it).

Do not merge the PR yourself. Do not create or push any `backend-v*`/`frontend-v*` tag yourself - that happens automatically in CI (`tag-release` job in `.github/workflows/ci-cd.yml`) once this PR is reviewed and merged into `main`.
