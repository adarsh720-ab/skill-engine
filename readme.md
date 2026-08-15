# Skill Path Engine

A graph-backed skill-gap and learning-pathway engine. Given a skill you know and a skill
you want, it computes the prerequisite chain(s) between them over a graph database — plus
a related-skills explorer and an admin panel for growing the graph.

Built as a take-home assignment: a Spring Boot 3 REST API on top of **CognoDB Cloud**
(a Bolt-protocol graph database, driver-compatible with Neo4j) with JWT auth and
role-based access control, paired with an Angular 18 frontend that visualizes the graph
with D3.

---

## Architecture

```
skill-engine/
├── render.yaml                  # Render Blueprint — deploys both services together
├── skillengine-backend/         # Spring Boot 3 / Java 17 REST API
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/wexa/skillengine/
│       ├── config/               # Security, JWT, CORS, DataSeeder
│       ├── controller/           # Auth, Skill, AdminSkill REST endpoints
│       ├── entity/               # SkillNode, UserNode (plain POJOs, no OGM)
│       ├── repository/           # Direct Cypher queries via the Neo4j Java driver
│       ├── service/               # Business logic
│       └── dto/                  # Request/response payloads
└── skillengine-frontend/        # Angular 18 SPA
    ├── Dockerfile
    ├── nginx.conf.template
    └── src/app/
        ├── core/                  # Services, guards, interceptors, models
        ├── shell/                 # Nav shell
        └── pages/                 # login, register, explorer, path-finder, admin
```

The backend talks to CognoDB directly over the **Bolt protocol** using the Neo4j Java
driver (CognoDB is Bolt/Cypher-compatible, so the standard Neo4j driver works against it
unmodified) — there's no ORM/OGM layer; queries are written by hand in the repository
classes for full control over graph traversal.

---

## Tech stack

**Backend**
- Java 17, Spring Boot 3.3.4
- Spring Security with stateless JWT auth (`jjwt`)
- Neo4j Java Driver (Bolt protocol) → CognoDB Cloud
- Bean Validation (`spring-boot-starter-validation`)
- JUnit 5 / Spring Security Test

**Frontend**
- Angular 18 (standalone components, new `@angular-devkit/build-angular:application` builder)
- D3.js — force-directed graph visualization
- RxJS, SCSS design tokens (deep navy / cyan-teal / amber-gold, Space Grotesk + Inter + JetBrains Mono)

**Deployment**
- Docker (multi-stage builds for both services)
- Render (Blueprint / Infrastructure-as-Code via `render.yaml`)
- Nginx serving the Angular production build

---

## Domain model

The graph has two node types:

| Node | Fields | Notes |
|---|---|---|
| `(:Skill)` | `name`, `category` | Categories include Frontend, Backend, Language, Framework, Database, DevOps, Cloud Platform, Security, Mobile, Data Science, and more |
| `(:User)` | `id`, `email`, `password` (BCrypt hash), `role` | Role is `ROLE_USER` or `ROLE_ADMIN` |

Skills are connected by `PREREQUISITE_FOR` edges, forming a directed prerequisite graph.
The seed data (`skills-seed.json`) ships **141 skills** across 19 categories, pre-wired
with realistic prerequisite chains (e.g. `HTML & CSS → JavaScript → React → Next.js`,
`Java Core → Spring Boot → Graph Databases`).

---

## Test credentials

These accounts are created automatically by `DataSeeder` the first time the backend
starts (`seed.enabled=true`, which is the default). Use them to log in and try out the
app without registering a new account first.

| Role | Email | Password |
|---|---|---|
| Admin (`ROLE_ADMIN`) | `admin@skillengine.dev` | `ChangeMe123!` |
| User (`ROLE_USER`) | `user@skillengine.dev` | `ChangeMe123!` |

The admin account can access the **Admin** panel (add skills + prerequisite edges) in
addition to everything the user account can do (Explorer, Path Finder).

> These are placeholder dev credentials, not real secrets — override them via
> `SEED_ADMIN_EMAIL`, `SEED_ADMIN_PASSWORD`, `SEED_USER_EMAIL`, `SEED_USER_PASSWORD`
> before pointing this at anything beyond a demo/throwaway instance.

---

## API reference

All routes are prefixed `/api/v1`. Protected routes require `Authorization: Bearer <jwt>`.

| Method | Path | Access | Purpose |
|---|---|---|---|
| `POST` | `/auth/register` | Public | Create a `ROLE_USER` account |
| `POST` | `/auth/login` | Public | Authenticate, get a JWT |
| `POST` | `/skills/path` | USER or ADMIN | Find prerequisite path(s) between two skills (1–5 hops) |
| `GET` | `/skills/related?query=&maxHops=` | USER or ADMIN | Explore skills related to a query, N hops out |
| `GET` | `/skills/search?query=&limit=` | USER or ADMIN | Search/autocomplete skill names |
| `POST` | `/admin` | ADMIN only | Create a new skill node + its `PREREQUISITE_FOR` edges |

Example — find a path:

```bash
curl -X POST http://localhost:8080/api/v1/skills/path \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"startSkill": "Java Core", "endSkill": "Graph Databases", "maxHops": 5}'
```

---

## Frontend pages

- **Login / Register** — JWT auth against `/api/v1/auth/**`
- **Explorer** — search-first landing; a stack list on the left stays in sync with a live
  D3 force-directed graph on the right (`/api/v1/skills/related`)
- **Path Finder** — renders every prerequisite chain between two skills as a sequential
  path, not a fan-out graph (`/api/v1/skills/path`)
- **Admin** — `ROLE_ADMIN`-only panel to add a skill and wire up its prerequisite edges
  (`/api/v1/admin`)

---

## Running locally (without Docker)

**Backend**
```bash
cd skillengine-backend
export COGNODB_URI=bolt+ssc://<your-cognodb-host>:7687
export COGNODB_USER=<user>
export COGNODB_PASSWORD=<password>
export JWT_SECRET=<a-long-random-string-at-least-32-chars>
mvn spring-boot:run
```
On first startup (`seed.enabled=true` by default), `DataSeeder` creates the 141-skill
graph plus two accounts:
- `admin@skillengine.dev` / `ChangeMe123!` (`ROLE_ADMIN`)
- `user@skillengine.dev` / `ChangeMe123!` (`ROLE_USER`)

Override these via `SEED_ADMIN_EMAIL`, `SEED_ADMIN_PASSWORD`, `SEED_USER_EMAIL`,
`SEED_USER_PASSWORD` before pointing this at anything but a throwaway instance.

**Frontend**
```bash
cd skillengine-frontend
npm install
npm start          # ng serve → http://localhost:4200
```
Edit `src/environments/environment.ts` if your backend isn't on `http://localhost:8080`.

---

## Running locally with Docker

**Backend**
```bash
cd skillengine-backend
docker build -t skillengine-api .
docker run -p 8080:8080 \
  -e COGNODB_URI="bolt+ssc://<your-cognodb-host>:7687" \
  -e COGNODB_USER="<user>" \
  -e COGNODB_PASSWORD="<password>" \
  -e JWT_SECRET="<a-long-random-string>" \
  skillengine-api
```

**Frontend**
```bash
cd skillengine-frontend
docker build --build-arg API_BASE_URL=http://localhost:8080 -t skillengine-ui .
docker run -p 4200:8080 skillengine-ui
```

Then open `http://localhost:4200`.

---

## Deploying to Render

Both services deploy together from the root `render.yaml` using Render's Blueprint
(Infrastructure-as-Code) flow:

1. Push this repo to GitHub.
2. In the Render dashboard: **New → Blueprint** → select the repo.
3. Render reads `render.yaml` and provisions two Docker-based web services:
    - `skillengine-api` — builds `skillengine-backend/Dockerfile` (Maven → JRE, multi-stage)
    - `skillengine-ui` — builds `skillengine-frontend/Dockerfile` (Node → Nginx, multi-stage)
4. You'll be prompted for the secret env vars (marked `sync: false` in `render.yaml`):
   `COGNODB_URI`, `COGNODB_USER`, `COGNODB_PASSWORD`, and the seed admin/user
   email + password. `JWT_SECRET` is auto-generated by Render.
5. The frontend's `API_BASE_URL` build arg is wired automatically to the backend
   service's live URL (`RENDER_EXTERNAL_URL`) via `fromService` — no manual URL
   entry or rebuild needed if the backend's URL ever changes.

Both services read `PORT` dynamically (`server.port: ${PORT:8080}` in
`application.yml`, and a templated Nginx config on the frontend), matching Render's
requirement that web services bind to the port it assigns at runtime.

---

## Known trade-offs (by design, for this assignment)

- **CORS is wide open** (`allowedOriginPatterns: "*"`) in `SecurityConfig` to unblock
  the Angular frontend without extra config during development. Combined with
  `allowCredentials(true)`, this should be narrowed to real origins before any
  production use beyond this assignment.
- No Spring Data Neo4j / OGM — repository classes issue Cypher directly via the
  driver, trading some boilerplate for full control over graph traversal queries.
- The `maxHops` bound on path traversal is capped at 5 and built into the Cypher
  query as a literal, matching the `*1..N` bound documented in the backend's own
  `README.md`.