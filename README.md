# GitScope

A fullstack **Git repository analytics dashboard**. Point it at any Git repo —
a local path on disk or a remote clone URL — and it walks the entire commit
history and renders a single-repo dashboard of charts and tables.

It merges duplicate author identities (e.g. `SameepSehgal`, `Sameep Sehgal`,
`sameep-sehgal-1` are treated as one person), so every metric counts each
contributor exactly once.

```
┌──────────────┐    POST /api/analyze     ┌──────────────────────────┐
│  React + Vite│ ───────────────────────▶ │  Spring Boot + JGit      │
│  (port 3000) │ ◀─────────────────────── │  (port 8080)             │
└──────────────┘   one AnalyticsResult     └──────────────────────────┘
```

The backend uses **JGit**, so no native `git` binary is required: remote URLs
are cloned into a temp directory (deleted afterwards) and local repos are
opened in place.

---

## Project layout

```
GitScope/
├── backend/      Spring Boot (Java 17, Maven) — JGit analysis engine + REST API
│   └── src/main/java/com/gitscope/
│       ├── controller/   ApiController, GlobalExceptionHandler
│       ├── service/      GitAnalyticsService, IdentityResolver
│       ├── model/        AnalyticsResult (the full JSON payload)
│       ├── dto/          AnalyzeRequest
│       └── config/       WebConfig (CORS), GitScopeProperties (alias map)
└── frontend/     React + Vite + Recharts — the dashboard UI
    └── src/
        ├── App.jsx, api.js, format.js, index.css
        └── components/   RepoInput, Dashboard, SummaryCards,
                          AuthorLeaderboard, Charts, Tables
```

---

## Prerequisites

- **Java 17+** and **Maven 3.6+** (backend)
- **Node 18+** and **npm** (frontend)

---

## Running

### 1. Backend (port 8080)

```bash
cd backend
mvn spring-boot:run
```

Endpoints:

| Method | Path           | Body                                   | Returns                |
|--------|----------------|----------------------------------------|------------------------|
| `GET`  | `/api/health`  | —                                      | `{"status":"ok"}`      |
| `POST` | `/api/analyze` | `{"path":"..."}` **or** `{"url":"..."}`| one `AnalyticsResult`  |

CORS is enabled for `http://localhost:3000` and `http://localhost:5173`.

Quick smoke test:

```bash
curl -s http://localhost:8080/api/analyze \
  -H 'Content-Type: application/json' \
  -d '{"path":"/absolute/path/to/a/repo"}'
```

### 2. Frontend (port 3000)

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:3000>. The Vite dev server proxies `/api` to
`http://localhost:8080`, so run the backend first. Use the toggle to switch
between **Local path** and **Remote URL**, paste a repo, and hit **Analyze**.

---

## What it analyzes

The history is walked **once** with `git.log().all()`. Merge commits
(`parentCount > 1`) are skipped, and per-commit diffs are computed against the
first parent (an empty-tree iterator for the root commit) via JGit's
`DiffFormatter` / `DiffEntry` / `Edit`. A single `AnalyticsResult` carries:

- **Repo summary** — name, total commits, total authors, lines added/removed,
  tracked files, first/last commit date, active days.
- **Per-author stats** — commits, added, removed, net, files touched, active
  days, and the list of raw aliases that merged into each contributor.
- **Commits by month** (`YYYY-MM`), **by hour** (`00`–`23`), and **by weekday**
  (Mon–Sun, fixed order).
- **File churn** — the 20 most-changed files.
- **Commit verbs** — the top 15 first words of commit messages.
- **Bus factor** — files owned 100% by a single author (files touched only once
  are ignored).
- **File-type breakdown** by extension, from the HEAD tree.
- **Largest 15 commits** by total lines changed.
- **Weekday-by-author** breakdown.

---

## Identity merging

`IdentityResolver` collapses duplicate author identities in two layers:

1. **Explicit alias map** — an optional, operator-configured
   `raw name → canonical name` table (case-insensitive). Configure it in
   `backend/src/main/resources/application.properties`:

   ```properties
   gitscope.aliases.SameepSehgal=Sameep Sehgal
   gitscope.aliases.sameep-sehgal-1=Sameep Sehgal
   ```

2. **Normalization fallback** — lowercase the name, strip the token `veersa`,
   then remove all spaces, hyphens and digits. Names that collapse to the same
   normalized form merge automatically. This alone handles
   `SameepSehgal` / `Sameep Sehgal` / `sameep-sehgal-1`.

The most human-readable name in a group (preferring one with a space) is chosen
as the canonical display name; every raw alias is preserved and exposed in the
leaderboard (expand a row to see them).

---

## Design notes

The UI is a **"terminal-meets-ledger"** developer tool: a dark slate canvas, a
single phosphor-green accent, monospace tabular figures for every numeric/data
cell, and a clean sans for prose. The boldness is spent on one signature
element — the glowing monthly commit-trend chart. It is responsive down to
mobile, has visible keyboard focus, and respects `prefers-reduced-motion`.

---

## Building for production

```bash
cd backend  && mvn clean package      # -> target/gitscope-backend-1.0.0.jar
cd frontend && npm run build          # -> dist/
```
