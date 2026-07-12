# Implementation Plan v2 — Lean & World-Class

This is the revised plan. It keeps the original architecture (schema-first, AI
generates data not UI, deterministic validation, local-first storage) and changes
what earns its keep in either direction: cut structure that doesn't pay off, add the
few things that make output world-class. Every change below is aimed at *lower build
and running cost* and *higher, more consistent lesson quality*.

## Changes from the original plan

### Leaner

| Area | Original | v2 | Why |
|------|----------|----|-----|
| AI pipeline | 5 stages (A–E) | 2 AI calls: analyze+plan, generate | Analysis and planning come from one reading of the source; the AI-validates-AI stage is replaced by the free deterministic validator + mandatory parent review. ~Halves generation cost. |
| JSON reliability | Generate then validate/retry | Structured outputs (forced tool call) | The provider *guarantees* schema-valid JSON, removing the malformed-JSON retry loop. |
| OCR | Backend/AI vision | On-device ML Kit OCR, vision only when the parent flags a diagram | Free, offline, and cuts the most expensive token category (images) to near zero. |
| Audio | `POST /api/audio/generate` | Android on-device TTS | Free, offline; endpoint removed from V1. |
| Storage | 11 Room tables incl. per-activity rows and version table | 5 tables; lesson stored as a JSON document | The schema *is* the data model; versioning is a new JSON row. Far less mapping code. |
| Android modules | 9 Gradle modules | 1 module, clean packages | Multi-module pays off for teams/large code; premature here. Split later if it hurts. |
| V1 activities | 14 types | 6 polished types | World-class = six that feel great, not fourteen that feel adequate. |

### World-class additions

1. **Golden-set eval harness** (`backend/eval/`). The biggest gap in the original
   plan: it validated *shape* but never measured *quality*. Lesson quality lives in
   the prompts, which change constantly. The harness regenerates a fixed set and
   scores each against a deterministic rubric (reading level vs age, source
   grounding, feedback quality, difficulty progression, variety). Run after any
   prompt change. Free and stable — no AI judging AI.
2. **Misconception-driven distractors.** The quality difference between a mediocre
   and a great question is almost entirely in the wrong answers (they must embody
   real misconceptions) and the incorrect-answer feedback (must teach, never "try
   again"). The analyze stage extracts misconceptions explicitly and carries them
   into generation. Both are enforced in the rubric.
3. **Prompt caching on the source summary.** Extend Lesson is the most frequent
   post-V1 operation and resends the source summary every time. The provider adapter
   marks that stable prefix cacheable (~10% of input price), making extensions
   nearly free.
4. **Model tier routing.** Cheap model for extraction/analysis; strong model only
   for generation where quality shows. With on-device OCR + caching, a full lesson
   lands around 1–3 cents and an extension well under a cent. Family-scale AI spend:
   a few dollars a month; Modal's free tier likely covers hosting.
5. **Delight is a requirement, not a nice-to-have.** <100ms haptic feedback,
   celebration on completion, read-aloud by default under age 8, and zero loading
   spinners in child mode (generation always happens at parent time — the flow
   already guarantees this; it's now a hard rule).

## The two quality gates (why dropping AI-validates-AI is safe)

1. **Deterministic validator** (`backend/app/validators/deterministic.py`): free,
   exhaustive, runs on every generated and extended lesson. Catches every
   *mechanical* defect — missing answers, duplicates, incomplete data, age
   violations, unsupported types.
2. **Parent review**: a human sees and approves every lesson before any child does.
   This is a stronger *pedagogical* check than a second AI pass, and it costs nothing.

## Cost model (rough, per operation)

- Full lesson: 1 cheap analyze call + 1 strong generate call ≈ **1–3¢**.
- Extension: 1 strong call with cached prefix ≈ **<1¢**.
- Validation, scoring, mastery, OCR, TTS: **free** (deterministic / on-device).

## Build order (unchanged, schema-first)

1. Finalize lesson schema ✅
2. Sample lessons ✅
3. Deterministic validator + tests ✅
4. Backend generation pipeline + eval harness ✅
5. Android renderer library (dispatcher + MC done; five stubs to fill)
6. Room + lesson player
7. Source processing (on-device OCR, PDF text extraction, vision-on-demand)
8. Parent preview/edit/approve
9. Extend Lesson (cached prefix, weak-concept targeting)
10. Narration, signed APK, multi-age testing via the eval harness

## V1 completion criteria

Parent creates a lesson from text/image/PDF → previews and approves → child completes
6+ activity types → progress and mastery persist locally → lesson extends without
duplicates → saved lessons replay offline → lesson exports as a private package.
