# Backend setup & API keys

## Do you need an Anthropic API key?

**Not to test the APK.** The app plays the bundled sample lessons fully offline —
renderers, player, scoring, and mastery all run on-device with no backend and no
key.

**Yes, later, for real lesson generation** — creating lessons from your own text,
photos, and PDFs. The key lives *only* on the backend as a secret. It is never in
the APK, never in the repo, and the phone never talks to Anthropic directly.

## Getting a key

1. Create an account at https://console.anthropic.com
2. Add a small amount of credit (at family scale, expect a few dollars per month —
   a full lesson costs roughly 1–3¢, an extension under 1¢).
3. Create an API key under **Settings → API keys**.

## Running the backend locally (quickest way to try real generation)

```bash
cd backend
pip install -r requirements.txt
export ANTHROPIC_API_KEY=sk-ant-...      # your key
export APP_SHARED_TOKEN=<any-long-random-string>
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Without `ANTHROPIC_API_KEY` set, the backend automatically uses the deterministic
fake provider — everything works, but lessons are canned samples.

## Deploying on Modal (the plan's hosting choice)

1. `pip install modal && modal setup`
2. Store the secrets once:
   `modal secret create kids-learning ANTHROPIC_API_KEY=sk-ant-... APP_SHARED_TOKEN=...`
3. A `modal_app.py` deployment wrapper is a next-step item; it attaches the secret
   and serves `app.main:app`. Modal's free tier comfortably covers family-scale
   usage.

## Connecting the app to the backend

The APK reads two values injected at build time (see `android/app/build.gradle.kts`):

- `BACKEND_BASE_URL` — your Modal (or local) HTTPS URL
- `APP_SHARED_TOKEN` — the same token you set on the backend

Set them as environment variables when building, or as GitHub Actions repository
secrets for CI builds. Rotating the token = rebuild the APK.

## Model choice and cost routing

The provider adapter routes by tier (`backend/app/providers/claude.py`):

- `small` → Haiku-class model: source analysis and planning
- `strong` → Sonnet-class model: lesson generation

Override with `CLAUDE_MODEL_SMALL` / `CLAUDE_MODEL_STRONG` env vars without code
changes.
