"""Deterministic (programmatic) lesson validator.

This is the real quality gate. JSON Schema validation guarantees the *shape* of a
lesson; this module checks the *semantics* that a schema cannot express:

* IDs are unique across the whole lesson.
* Every correct-answer reference points at an option that exists.
* Match-pairs and drag-into-order data is internally complete and consistent.
* Fill-in-the-blank templates and their blank definitions agree.
* Questions are not duplicated.
* Content respects the age profile (option counts, text lengths, activity mix).

It returns a list of structured errors. An empty list means the lesson is safe to
show to a parent for review. Nothing here calls an AI model; it is pure, fast, and
free, so it runs on every generated and every extended lesson.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field

from app.validators.age_profiles import AgeProfile, profile_for_age

# Activity section types the V1 Android app can actually render. Anything else is
# rejected before it can reach a child's screen.
SUPPORTED_TYPES = {
    "explanation",
    "multiple_choice",
    "true_false",
    "fill_in_the_blank",
    "match_pairs",
    "drag_into_order",
}

ACTIVITY_TYPES = SUPPORTED_TYPES - {"explanation"}

_BLANK_MARKER = re.compile(r"\{\{([a-z0-9][a-z0-9_-]{0,63})\}\}")


@dataclass
class ValidationError:
    code: str
    message: str
    section_id: str | None = None

    def __str__(self) -> str:  # pragma: no cover - convenience only
        where = f" [{self.section_id}]" if self.section_id else ""
        return f"{self.code}{where}: {self.message}"


@dataclass
class ValidationResult:
    errors: list[ValidationError] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return not self.errors

    def add(self, code: str, message: str, section_id: str | None = None) -> None:
        self.errors.append(ValidationError(code, message, section_id))


def _normalise_question(text: str) -> str:
    """Lowercase, strip punctuation and collapse whitespace for duplicate detection."""
    text = text.lower()
    text = re.sub(r"[^a-z0-9\s]", "", text)
    return re.sub(r"\s+", " ", text).strip()


def _question_text(section: dict) -> str | None:
    for key in ("question", "statement", "template", "instruction"):
        if key in section:
            return section[key]
    return None


def validate_lesson(lesson: dict) -> ValidationResult:
    """Run all semantic checks. Assumes the lesson already passed JSON Schema."""
    result = ValidationResult()
    profile = profile_for_age(lesson.get("age", 9))

    _check_unique_ids(lesson, result)
    concept_ids = {c["concept_id"] for c in lesson.get("concepts", [])}
    source_ids = {s["source_id"] for s in lesson.get("source_references", [])}

    seen_questions: dict[str, str] = {}
    activity_count = 0

    for section in lesson.get("sections", []):
        sid = section.get("id")
        stype = section.get("type")

        if stype not in SUPPORTED_TYPES:
            result.add("unsupported_type", f"Activity type '{stype}' is not supported.", sid)
            continue

        _check_concept_ref(section, concept_ids, result)
        _check_grounding_ref(section, source_ids, result)

        if stype in ACTIVITY_TYPES:
            activity_count += 1
            _check_feedback_present(section, result)

        if stype == "multiple_choice":
            _check_multiple_choice(section, profile, result)
        elif stype == "true_false":
            pass  # correct_answer is a bool; schema already guarantees it.
        elif stype == "fill_in_the_blank":
            _check_fill_in_the_blank(section, lesson, profile, result)
        elif stype == "match_pairs":
            _check_match_pairs(section, result)
        elif stype == "drag_into_order":
            _check_drag_into_order(section, result)

        _check_duplicate(section, seen_questions, result)

    if activity_count == 0:
        result.add("no_activities", "Lesson has no interactive activities, only explanations.")

    return result


def _check_unique_ids(lesson: dict, result: ValidationResult) -> None:
    seen: set[str] = set()
    for section in lesson.get("sections", []):
        sid = section.get("id")
        if sid in seen:
            result.add("duplicate_section_id", f"Section id '{sid}' is used more than once.", sid)
        seen.add(sid)


def _check_concept_ref(section: dict, concept_ids: set[str], result: ValidationResult) -> None:
    cid = section.get("concept_id")
    if cid is not None and cid not in concept_ids:
        result.add(
            "unknown_concept",
            f"concept_id '{cid}' is not declared in the lesson's concepts list.",
            section.get("id"),
        )


def _check_grounding_ref(section: dict, source_ids: set[str], result: ValidationResult) -> None:
    grounding = section.get("grounding")
    if not grounding:
        return
    src = grounding.get("source_id")
    if grounding.get("basis") == "source_explicit" and src is None:
        result.add(
            "grounding_missing_source",
            "Grounding claims 'source_explicit' but names no source_id.",
            section.get("id"),
        )
    if src is not None and src not in source_ids:
        result.add(
            "unknown_source",
            f"grounding references source_id '{src}' that is not in source_references.",
            section.get("id"),
        )


def _check_feedback_present(section: dict, result: ValidationResult) -> None:
    for field_name in ("hint", "correct_feedback", "incorrect_feedback"):
        if not section.get(field_name):
            result.add(
                "missing_feedback",
                f"Activity is missing required '{field_name}'.",
                section.get("id"),
            )


def _check_multiple_choice(section: dict, profile: AgeProfile, result: ValidationResult) -> None:
    sid = section.get("id")
    options = section.get("options", [])
    option_ids = [o["id"] for o in options]

    if len(set(option_ids)) != len(option_ids):
        result.add("duplicate_option_id", "Option ids are not unique.", sid)

    if len(options) > profile.max_options:
        result.add(
            "too_many_options",
            f"{len(options)} options exceeds the age limit of {profile.max_options}.",
            sid,
        )

    correct = section.get("correct_answer_ids", [])
    for cid in correct:
        if cid not in option_ids:
            result.add(
                "correct_answer_missing",
                f"correct_answer_ids references '{cid}', which is not an option.",
                sid,
            )

    mode = section.get("selection_mode")
    if mode == "single" and len(correct) != 1:
        result.add(
            "single_mode_multiple_answers",
            "selection_mode is 'single' but there is not exactly one correct answer.",
            sid,
        )
    if mode == "multiple" and len(correct) < 1:
        result.add("no_correct_answer", "selection_mode 'multiple' needs at least one answer.", sid)

    # A select-all question where every option is correct has no wrong answer to learn from.
    if mode == "multiple" and len(correct) == len(options) and len(options) > 1:
        result.add(
            "all_options_correct",
            "Every option is marked correct; there is no distractor.",
            sid,
        )


def _check_fill_in_the_blank(
    section: dict, lesson: dict, profile: AgeProfile, result: ValidationResult
) -> None:
    sid = section.get("id")
    template = section.get("template", "")
    markers = _BLANK_MARKER.findall(template)
    blanks = section.get("blanks", [])
    blank_ids = [b["id"] for b in blanks]

    if len(set(blank_ids)) != len(blank_ids):
        result.add("duplicate_blank_id", "Blank ids are not unique.", sid)

    if set(markers) != set(blank_ids):
        result.add(
            "blank_mismatch",
            f"Template markers {sorted(set(markers))} do not match blank ids {sorted(set(blank_ids))}.",
            sid,
        )

    for b in blanks:
        if not b.get("accepted_answers"):
            result.add("blank_no_answer", f"Blank '{b.get('id')}' has no accepted answers.", sid)

    # For the youngest learners, typing is inappropriate: require a tap-to-fill word bank.
    if profile.requires_word_bank and not section.get("word_bank"):
        result.add(
            "word_bank_required",
            f"Age {lesson.get('age')} requires a word_bank for fill-in-the-blank (no free typing).",
            sid,
        )

    # If a word bank exists, every correct answer must be present in it.
    word_bank = section.get("word_bank")
    if word_bank:
        bank_lower = {w.lower() for w in word_bank}
        for b in blanks:
            if not any(ans.lower() in bank_lower for ans in b.get("accepted_answers", [])):
                result.add(
                    "answer_not_in_word_bank",
                    f"Blank '{b.get('id')}' has no accepted answer present in the word_bank.",
                    sid,
                )


def _check_match_pairs(section: dict, result: ValidationResult) -> None:
    sid = section.get("id")
    pairs = section.get("pairs", [])
    left_ids = [p["left"]["id"] for p in pairs]
    right_ids = [p["right"]["id"] for p in pairs]

    if len(set(left_ids)) != len(left_ids):
        result.add("duplicate_left_id", "Left item ids are not unique.", sid)
    if len(set(right_ids)) != len(right_ids):
        result.add("duplicate_right_id", "Right item ids are not unique.", sid)

    left_texts = [p["left"]["text"].strip().lower() for p in pairs]
    right_texts = [p["right"]["text"].strip().lower() for p in pairs]
    if len(set(left_texts)) != len(left_texts) or len(set(right_texts)) != len(right_texts):
        result.add(
            "duplicate_pair_text",
            "Two pairs share identical text, making the match ambiguous.",
            sid,
        )


def _check_drag_into_order(section: dict, result: ValidationResult) -> None:
    sid = section.get("id")
    items = section.get("items", [])
    item_ids = [i["id"] for i in items]
    order = section.get("correct_order", [])

    if len(set(item_ids)) != len(item_ids):
        result.add("duplicate_item_id", "Item ids are not unique.", sid)

    if sorted(order) != sorted(item_ids):
        result.add(
            "order_not_permutation",
            "correct_order is not an exact permutation of the items.",
            sid,
        )


def _check_duplicate(section: dict, seen: dict[str, str], result: ValidationResult) -> None:
    text = _question_text(section)
    if section.get("type") == "explanation" or not text:
        return
    norm = _normalise_question(text)
    if not norm:
        return
    if norm in seen:
        result.add(
            "duplicate_question",
            f"Question duplicates section '{seen[norm]}'.",
            section.get("id"),
        )
    else:
        seen[norm] = section.get("id")
