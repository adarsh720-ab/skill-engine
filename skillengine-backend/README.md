# Skill Path Engine — Backend

Spring Boot 3 / Java 17 backend for computing skill-gap learning pathways over a graph
database (CognoDB Cloud, Bolt protocol — driver-compatible with Neo4j).

## Run locally

```bash
export COGNODB_URI=bolt://<your-cognodb-host>:7687
export COGNODB_USER=<user>
export COGNODB_PASSWORD=<password>
export JWT_SECRET=<a-long-random-string-at-least-32-chars>

mvn spring-boot:run
```

On startup, `DataSeeder` creates:
- `admin@skillengine.dev` / `ChangeMe123!` (ROLE_ADMIN)
- `user@skillengine.dev` / `ChangeMe123!` (ROLE_USER)
- A small seed skill graph (HTML & CSS → JavaScript → React, Java Core → Spring Boot → Graph Databases)

**Change the seed passwords via env vars (`SEED_ADMIN_PASSWORD`, etc.) before running against
anything other than a throwaway local instance.**

## Endpoints

| Method | Path                  | Access          | Purpose                          |
|--------|-----------------------|-----------------|-----------------------------------|
| POST   | `/api/v1/auth/register` | Public        | Create a ROLE_USER account       |
| POST   | `/api/v1/auth/login`    | Public        | Get a JWT                        |
| POST   | `/api/v1/skills/path`   | USER or ADMIN | Multi-hop skill path traversal   |
| POST   | `/api/v1/admin`         | ADMIN only    | Create a skill + prerequisite edges |

Send the JWT from login/register as `Authorization: Bearer <token>` on the protected routes.

## Example: find a path

```bash
curl -X POST http://localhost:8080/api/v1/skills/path \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"startSkill": "Java Core", "endSkill": "Graph Databases", "maxHops": 5}'
```

## Notes / known trade-offs (read before treating this as done)

- **CORS is wide open (`origins = "*"`)** per the spec, to unblock an Angular frontend during
  development. Combined with `allowCredentials(true)` in `SecurityConfig`, this needs to be
  narrowed to real origins before any production deployment.
- The maxHops bound in the Cypher traversal (`*1..N`) is built as a string literal because
  Bolt doesn't allow parameterizing the hop-count in a variable-length pattern. The value is
  clamped server-side to 1–5 before it ever reaches the query string, so this isn't user
  input reaching the query unvalidated — but it's worth knowing this one query is the
  exception to "always use `Values.parameters(...)`" and why.
- No Spring Data Neo4j / OGM — this talks to the driver directly (`Session.run(...)`) so the
  Cypher is fully visible and controllable, which matters for a benchmarking-oriented project.
- No refresh-token flow, no rate limiting on `/auth/login`, no BOLT connection retry/backoff
  policy beyond the driver defaults. Fine for an assignment/benchmark; would need hardening
  for real production traffic.
