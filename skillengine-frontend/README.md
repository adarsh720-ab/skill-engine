# SkillEngine UI (Angular)

Frontend for the SkillEngine skill-graph explorer (WEXA assignment), built to talk
to the Spring Boot + CognoDB backend in `skillengine/`.

## What's here

- **Login / Register** — JWT auth against `/api/v1/auth/**`.
- **Explorer** — search-first landing that collapses into a two-pane view: a
  stack list on the left, synced to a live D3 force-directed graph on the
  right (`/api/v1/skills/related`).
- **Path Finder** — finds every prerequisite chain between two skills and
  renders each as a sequential chain, not a fan-out (`/api/v1/skills/path`).
- **Admin** — ROLE_ADMIN-only panel to add a new skill + its
  `PREREQUISITE_FOR` edges (`/api/v1/admin`).

Design tokens (deep navy base, cyan-teal accent, amber-gold highlight,
Space Grotesk / Inter / JetBrains Mono) live in `src/styles.scss`.

## Setup

```bash
npm install
```

Point the app at your backend:

- **Local dev** — edit `src/environments/environment.ts` if your backend
  isn't on `http://localhost:8080`.
- **Production build** — edit `src/environments/environment.prod.ts` and set
  `apiBaseUrl` to your deployed backend, e.g.
  `https://skillengine-api.onrender.com/api/v1`.

Run it:

```bash
npm start          # ng serve — http://localhost:4200
npm run build:prod # production build -> dist/skillengine-ui
```

## Backend prerequisites

1. Your Spring Boot backend must be running first (`seed.enabled=true` at
   least once so CognoDB has the skill graph + default admin/user accounts).
2. Backend CORS is already wide open (`allowedOriginPatterns: "*"` in
   `SecurityConfig`) — no backend changes needed for local dev. Before you
   ship this, tighten that to your actual frontend origin.
3. Default seeded accounts (unless you overrode the env vars):
   - Admin: `admin@skillengine.dev` / `ChangeMe123!`
   - User: `user@skillengine.dev` / `ChangeMe123!`

## Notes on things that were mid-fix in the original chat

- All reactive-form components (`login`, `register`, `path-finder`, `admin`)
  use `private readonly fb = inject(FormBuilder);` as a field initializer
  (not constructor injection) specifically to avoid the `TS2729: Property
  'fb' is used before its initialization` error that came up when a form
  group field was declared above a constructor-injected `fb`.
- The path-finder graph renders each selected path as an explicit sequential
  chain (`A → B → C → D`) via `pathMode` on `<app-graph-canvas>`, not the
  explorer's root-fans-out-to-related-skills layout.

## Deploying the frontend

This is a static SPA after `npm run build:prod` (output in `dist/skillengine-ui/browser`).
Any static host works — Vercel, Netlify, Render static site, GitHub Pages.

**Important:** Angular's client-side router will 404 on a hard refresh of
`/explorer`, `/path-finder`, etc. unless the host rewrites all paths to
`index.html`.

- **Vercel** — add a `vercel.json` with a catch-all rewrite to `/index.html`.
- **Netlify** — add `public/_redirects` containing `/* /index.html 200`.
- **Render (static site)** — set the rewrite rule `/* -> /index.html` in the
  dashboard.

Remember to set `apiBaseUrl` in `environment.prod.ts` to your deployed
backend's public URL before building, and once your backend has seeded data
once, set `SEED_ENABLED=false` there as discussed.

## Project structure

```
src/app/
  core/
    models/skill.model.ts        # mirrors backend DTOs exactly
    services/auth.service.ts     # signal-based auth state + localStorage persistence
    services/skill.service.ts    # wraps /related, /path, /admin
    interceptors/auth.interceptor.ts
    guards/auth.guard.ts
    guards/admin.guard.ts
  shell/nav/                     # persistent top nav
  pages/
    login/
    register/
    explorer/
      graph-canvas.component.ts  # D3 force-directed graph (shared by explorer + path-finder)
      explorer.component.ts
    path-finder/
    admin/
```
