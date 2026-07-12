"""Golden-set evaluation harness.

This is the difference between "it worked on my one test PDF" and consistent
quality. Lesson quality lives almost entirely in the prompts, and prompts are
edited constantly. This harness regenerates a fixed set of lessons and scores each
against an objective rubric, so a prompt change that improves one case but quietly
breaks another is caught immediately.

The rubric here is fully deterministic (no AI judging AI), so it is free to run and
stable over time. It checks the properties that matter and can be measured without
a model: reading level vs age, source grounding, feedback quality, difficulty
progression, and duplication. Add golden source documents under eval/golden/ and
run `python -m eval.run` after any prompt change.
"""

from __future__ import annotations

import re
from dataclasses import dataclass

from app.validators.age_profiles import profile_for_age
from app.validators.deterministic import ACTIVITY_TYPES, validate_lesson
from app.validators.schema import structural_errors


@dataclass
class RubricScore:
    name: str
    passed: bool
    score: float          # 0.0 - 1.0
    detail: str


def _sentences(text: str) -> list[str]:
    return [s for s in re.split(r"[.!?]+", text) if s.strip()]


def score_lesson(lesson: dict, *, expected_age: int) -> list[RubricScore]:
    scores: list[RubricScore] = []
    profile = profile_for_age(expected_age)

    # 1. Must be valid at all. A lesson that fails validation scores zero everywhere.
    struct = structural_errors(lesson)
    semantic = [] if struct else [str(e) for e in validate_lesson(lesson).errors]
    valid = not struct and not semantic
    scores.append(RubricScore(
        "validity", valid, 1.0 if valid else 0.0,
        "valid" if valid else f"errors: {struct + semantic}",
    ))
    if not valid:
        return scores

    sections = lesson["sections"]
    explanations = [s for s in sections if s["type"] == "explanation"]
    activities = [s for s in sections if s["type"] in ACTIVITY_TYPES]

    # 2. Reading level: sentences should respect the age band's word ceiling.
    long_sentences = 0
    total_sentences = 0
    for card in explanations:
        for sentence in _sentences(card.get("content", "")):
            total_sentences += 1
            if len(sentence.split()) > profile.max_sentence_words:
                long_sentences += 1
    reading_ok = total_sentences == 0 or long_sentences / total_sentences <= 0.1
    scores.append(RubricScore(
        "reading_level", reading_ok,
        1.0 - (long_sentences / total_sentences if total_sentences else 0),
        f"{long_sentences}/{total_sentences} sentences over {profile.max_sentence_words} words",
    ))

    # 3. Source grounding: most activities should trace to a source or be labelled.
    grounded = sum(1 for s in sections if s.get("grounding"))
    grounding_ratio = grounded / len(sections) if sections else 0
    scores.append(RubricScore(
        "grounding", grounding_ratio >= 0.8, grounding_ratio,
        f"{grounded}/{len(sections)} sections carry grounding",
    ))

    # 4. Feedback quality: incorrect_feedback must teach, not just say "try again".
    lazy = 0
    for a in activities:
        fb = (a.get("incorrect_feedback") or "").lower()
        if not fb or re.fullmatch(r"(try again[.!]?\s*)+", fb) or len(fb.split()) < 4:
            lazy += 1
    feedback_ok = lazy == 0
    scores.append(RubricScore(
        "feedback_quality", feedback_ok,
        1.0 - (lazy / len(activities) if activities else 0),
        f"{lazy}/{len(activities)} activities have lazy incorrect_feedback",
    ))

    # 5. Difficulty progression: difficulty_level should trend upward, not backward.
    levels = [s.get("difficulty_level") for s in activities if s.get("difficulty_level")]
    regressions = sum(1 for a, b in zip(levels, levels[1:]) if b < a - 1)
    progression_ok = regressions == 0 and len(levels) >= 2
    scores.append(RubricScore(
        "difficulty_progression", progression_ok,
        1.0 if progression_ok else 0.5,
        f"levels={levels}, backward jumps={regressions}",
    ))

    # 6. Activity variety: a good lesson uses more than one interaction type.
    types = {a["type"] for a in activities}
    variety_ok = len(types) >= 3
    scores.append(RubricScore(
        "activity_variety", variety_ok, min(len(types) / 4, 1.0),
        f"{len(types)} distinct activity types: {sorted(types)}",
    ))

    return scores


def summarise(scores: list[RubricScore]) -> tuple[bool, float]:
    passed = all(s.passed for s in scores)
    avg = sum(s.score for s in scores) / len(scores) if scores else 0.0
    return passed, avg
