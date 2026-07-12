"""Prompt construction for the two-stage generation pipeline.

The pipeline was deliberately reduced from five AI stages to two:

* Stage 1 (analyze + plan): read the source, extract concepts/facts/vocabulary,
  and produce the teaching plan in a single call. Analysis and planning come from
  the same reading of the material, so splitting them wasted a call.
* Stage 2 (generate): turn the plan into a validated lesson.

The old "AI validates the AI" stage was dropped. Its job is covered more cheaply
and more reliably by the deterministic validator (free, exhaustive) plus the
mandatory parent review (a human sees every lesson before a child does).

Two quality levers live in these prompts because they are where lesson quality is
actually won:

* Distractors must embody real misconceptions, not be obviously silly.
* Incorrect-answer feedback must teach toward the right idea, never just "try again".
"""

from __future__ import annotations

from app.validators.age_profiles import AgeProfile, profile_for_age

_QUALITY_RULES = """\
Quality rules you must follow:
- Ground every fact. If it is stated in the source, mark grounding.basis as
  "source_explicit" and cite the source_id and page(s). If it is standard
  knowledge not in the source, use "general_knowledge". If you reasoned it from
  the source, use "interpretation". Never invent facts not supported by either
  the source or well-established general knowledge.
- Wrong answers (distractors) must be plausible mistakes a learner of this age
  would actually make, drawn from the common misconceptions you identified. Never
  write joke options or obviously-absurd choices.
- incorrect_feedback must gently teach toward the correct idea and name the likely
  misconception. It must never be a bare "try again".
- correct_feedback should briefly reinforce *why* the answer is right.
- Every question must have a hint that nudges without giving away the answer.
- Do not repeat questions. Each activity must test something distinct.
- Uploaded images (if any) ARE the source material: read them fully, including
  diagrams, tables, and handwriting.
- CRITICAL image rule: when a section teaches from or asks about a graph,
  pictograph, chart, table, diagram, or picture that appears in an uploaded
  image, you MUST set that section's "image_ref" to the image's 0-based index --
  the child has to SEE it to answer. Use "image_description" ONLY for things NOT
  visible in any uploaded image, and never set both on the same section. Only
  reference images that were actually provided; never invent an index.
- You can CREATE data visuals natively — no image needed. Use a "chart" section
  (chart_type "bar" or "pictograph"; items with label/value/emoji; for
  pictographs set symbol_value and give every item an emoji symbol, with each
  value a multiple of symbol_value) to present data as a colourful graph, then
  ask questions about it. PREFER recreating a graph from the source as a chart
  section over attaching its photo — it is clearer for the child. Use the
  "build_bar_chart" activity (items with label/target, plus max_value) to let
  the child BUILD a graph by dragging bars to the right heights.
- REAL PHOTOS on demand: any section may set "image_search" to a concrete noun
  phrase matching a Wikipedia article title ("Jupiter", "Bengal tiger", "Great
  Pyramid of Giza"). The app fetches that article's lead photo and shows it with
  the section. Use it whenever seeing the real thing helps (planets, animals,
  landmarks, plants) and no uploaded image covers it. Keep terms unambiguous.
- "tap_image" activity: the child taps the correct picture. Give each option an
  "image_search" for a real photo AND an "emoji" fallback. Great for "which one
  is Saturn?" style questions.
- "sort_into_categories": drag items into 2-4 labelled buckets (living/non-living,
  inner/outer planets, nouns/verbs). Fits almost every subject.
- "number_line": the child slides a marker to the answer (min_value, max_value,
  step, correct_value). The go-to for estimation and counting questions.
- Give every section a relevant "emoji" (e.g. ☀️ for evaporation) and add an
  "emoji" to options where a picture helps (especially for young children). The
  emoji is the visual for that content. NEVER use an emoji that reveals the
  answer (no numbered emojis on ordering items, no ✓/✗ hints on options).
"""


def _age_rules(profile: AgeProfile, age: int) -> str:
    typing = (
        "Do NOT ask the child to type. For fill-in-the-blank always provide a "
        "word_bank of tappable choices."
        if profile.requires_word_bank
        else "Short typed answers are acceptable where appropriate."
    )
    return f"""\
Age adaptation (child is {age}, band '{profile.band}'):
- Use at most {profile.max_options} answer options per question.
- Keep sentences under about {profile.max_sentence_words} words.
- Keep each explanation card under about {profile.max_content_chars} characters.
- Reading support target: {profile.reading_support}.
- {typing}
"""


ANALYZE_SYSTEM = """\
You are an expert curriculum analyst and instructional designer for children. You
read supplied educational material and produce a structured analysis and teaching
plan. You are precise about what the source actually says versus general knowledge.
"""


def build_analyze_prompt(*, source_text: str, topic: str, age: int,
                         subject: str, objective: str, language: str) -> str:
    profile = profile_for_age(age)
    return f"""\
Analyse the material below and produce a teaching plan for a {age}-year-old.

Subject: {subject}
Topic: {topic}
Learning objective: {objective}
Language for the lesson: {language}

{_age_rules(profile, age)}

Produce:
1. The main concepts (each with a short stable snake_case id).
2. Key facts, vocabulary, definitions, and examples, each tagged with whether it
   is explicit in the source or general knowledge.
3. The common misconceptions a child this age holds about these concepts. These
   will become the distractors, so be specific.
4. The order concepts should be taught, including any prerequisites.
5. A recommended progression of activities from easy to challenging.

SOURCE MATERIAL (any attached images are also source material -- read them fully,
including diagrams, tables, and handwriting):
\"\"\"
{source_text}
\"\"\"
"""


LESSON_SYSTEM = f"""\
You are an expert children's lesson author. You turn a teaching plan into a
complete, structured, interactive lesson. You output only data that conforms to
the provided schema.

{_QUALITY_RULES}
"""


def build_lesson_prompt(*, plan: str, topic: str, age: int, subject: str,
                        difficulty: str, language: str, num_questions: int,
                        objective: str) -> str:
    profile = profile_for_age(age)
    return f"""\
Using the teaching plan below, write a complete lesson for a {age}-year-old.

Subject: {subject}
Topic: {topic}
Learning objective: {objective}
Overall difficulty: {difficulty}
Language: {language}
Aim for about {num_questions} interactive activities.

{_age_rules(profile, age)}

OPEN the lesson with ONE "animated_story" section: 3-6 short scenes that tell the
core idea as a tiny story the app plays with animation, narration, and sounds.
Each scene is one or two short sentences plus a visual (a big emoji, an
image_search photo, or an uploaded image_ref). Make it warm and concrete — meet a
character, watch something happen — not a list of facts.

After the story, structure the lesson as: alternating explanation cards and
activities that build from difficulty_level 1 upward, ending with a challenge
activity and a warm completion_message. Use only these section types:
animated_story, explanation, chart, multiple_choice, true_false,
fill_in_the_blank, match_pairs, drag_into_order, build_bar_chart, tap_image,
sort_into_categories, number_line.
Vary the activity types so the lesson stays fresh. Give every section a unique
snake_case id.
Every activity must reference a concept_id declared in the concepts list.

TEACHING PLAN:
\"\"\"
{plan}
\"\"\"
"""


EXTEND_SYSTEM = LESSON_SYSTEM


def build_extend_prompt(*, extension_type: str, num_activities: int,
                        objectives: list[str], existing_questions: list[str],
                        weak_concepts: list[str], age: int, difficulty: str,
                        language: str) -> str:
    profile = profile_for_age(age)
    weak = ", ".join(weak_concepts) if weak_concepts else "none reported"
    existing = "\n".join(f"- {q}" for q in existing_questions) or "(none)"
    return f"""\
Generate {num_activities} additional activities to extend an existing lesson.
Extension type requested: {extension_type}.

Learning objectives to stay within:
{chr(10).join(f'- {o}' for o in objectives)}

Child's currently weak concepts to prioritise: {weak}
Continue from difficulty '{difficulty}'. Language: {language}.

{_age_rules(profile, age)}

Do NOT duplicate any of these existing questions (matching by meaning, not just
wording):
{existing}

Return new activities only, each with a unique snake_case id, following all the
quality rules.
"""
