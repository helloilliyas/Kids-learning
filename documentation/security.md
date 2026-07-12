# Security & Privacy

The app is private and family-scale. The design keeps child data on the device and
keeps all AI credentials on the backend.

## Secrets

- **AI API keys** (`ANTHROPIC_API_KEY`) live only as backend secrets (e.g. Modal
  secrets). They are never in the APK, the repo, or any client request.
- **App shared token** (`APP_SHARED_TOKEN`) authenticates the private APK to the
  backend. It is a bearer token injected at build time via `BuildConfig`, not an AI
  key. For a single-family private app this is sufficient. Rotate by rebuilding.
- Nothing secret is committed; `.gitignore` excludes `.env`, keystores, and
  `local.properties`.

## Data handling

- Child progress, mastery, lessons, and uploaded files stay **local** (Room + app
  file storage). No cloud database, no login, no analytics on the child.
- The backend is **stateless**: it processes an upload, returns lesson JSON, and
  deletes temporary files after processing. It stores no child data.
- All backend traffic is over **HTTPS**.
- The app avoids collecting names, school details, location, or photographs of the
  child.

## Child-mode safety

- No open-ended AI chat is exposed in child mode.
- Every lesson requires **parent approval** before it can be shown to a child
  (`LessonEntity.approved`).
- Parent mode is gated by a PIN (`AppSettingsEntity.parentPinHash`).

## Content grounding

Every activity records where its content came from (`grounding.basis`:
`source_explicit` / `general_knowledge` / `interpretation`) plus source id and pages,
so a parent can tap "View Source" and verify against the original material. The
deterministic validator rejects a `source_explicit` claim that names no source.
