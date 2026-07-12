# Kids Learning — AI-Powered Interactive Education App

A private Android app that turns any material a parent supplies (typed text, photos,
camera scans, or PDFs) into a complete, age-appropriate interactive lesson. The
parent reviews and approves every lesson; the child only ever sees approved content.
All child data stays on the phone.

## The core idea

**The AI generates data, never UI.** The product is two halves that meet at a
strict, versioned contract:

1. A **Lesson JSON schema + generation pipeline** (backend) produces validated,
   source-grounded lesson data.
2. A **native activity-renderer library** (Android) renders any valid lesson JSON.

Adding a new topic never requires an app update, and the whole app can be developed
and demoed against fixture lessons with no AI in the loop.

```
Upload (text/PDF/image)
  → Analyze + Plan (1 AI call, cheap model)
  → Generate lesson (1 AI call, strong model, structured output)
  → Deterministic validator (free, exhaustive)   ← the real quality gate
  → Parent review / edit / approve                ← the human quality gate
  → Child plays lesson (activities rendered from JSON)
  → On-device mastery tracking (deterministic)
  → Extend Lesson (no duplicates, targets weak concepts, cached source)
```

## Repository layout

```
lesson-schema/     Shared contract: lesson.schema.json + example lessons (the fixtures)
backend/           FastAPI: providers, prompts, processors, validators, eval harness
android/           Single-module Android app (Kotlin, Compose, Room)
documentation/     Plan (v2), architecture, backend and security notes
```

## What works right now

- **Lesson schema v1.0** with six activity types, source grounding, and age metadata.
- **Three example lessons** (ages 5, 9, 12) covering all six activity types.
- **Deterministic validator** with 19 unit tests: catches missing answers, duplicate
  questions, age-inappropriate option counts, incomplete match/order data, and more.
- **FastAPI backend** with `generate` / `validate` / `health` / `schema/version`
  endpoints, a swappable AI provider adapter (Claude + a deterministic fake), and a
  two-stage generation pipeline — all runnable offline via the fake provider.
- **Golden-set eval harness** scoring lessons on reading level, grounding, feedback
  quality, difficulty progression, and variety. The three examples score 0.96–0.99.
- **Android scaffold**: domain models mirroring the schema, on-device deterministic
  scoring, a per-concept mastery engine, a lean 5-table Room layer, and the renderer
  dispatcher with one full renderer (multiple choice) plus stubs for the other five.

## Run the backend (offline, no API key)

```bash
cd backend
pip install -r requirements.txt
python -m pytest -q                 # 24 tests
python -m eval.run                  # score the example lessons
uvicorn app.main:app --reload       # serve the API (uses the fake provider)
```

To use real generation, set `ANTHROPIC_API_KEY` (a backend secret — never shipped in
the APK). To require the app token, set `APP_SHARED_TOKEN`.

## Build the Android app

The Android module needs the Android SDK, which is not present in the CI/dev
container used to author this scaffold, so it has not been compiled here. With
Android Studio (or a machine with the SDK + `ANDROID_HOME` set):

```bash
cd android
./gradlew assembleDebug            # requires Android SDK 34
```

## Design decisions

See `documentation/plan-v2.md` for the full lean/world-class plan and the rationale
for each change from the original proposal (two AI stages instead of five, structured
outputs, on-device OCR/TTS, five Room tables instead of eleven, one module instead of
nine, six polished activities instead of fourteen, plus the eval harness and prompt
caching).
