"""Unit tests for the deterministic validator's failure detection.

Each test starts from a valid lesson and breaks exactly one thing, proving the
validator catches that specific class of defect. This is the safety net that lets
us trust AI-generated lessons without an AI second-opinion in the loop.
"""

from __future__ import annotations

import copy

import pytest

from app.validators.deterministic import validate_lesson


def _codes(lesson: dict) -> set[str]:
    return {e.code for e in validate_lesson(lesson).errors}


@pytest.fixture
def base_lesson() -> dict:
    return {
        "schema_version": "1.0",
        "lesson_id": "l1",
        "title": "Test",
        "subject": "Science",
        "language": "en",
        "age": 9,
        "difficulty": "beginner",
        "learning_objectives": ["Learn"],
        "concepts": [{"concept_id": "c1", "name": "Concept One"}],
        "source_references": [{"source_id": "s1", "title": "Book", "pages": [1]}],
        "completion_message": "Done!",
        "sections": [
            {
                "id": "e1",
                "type": "explanation",
                "concept_id": "c1",
                "title": "Intro",
                "content": "Hello.",
                "grounding": {"basis": "source_explicit", "source_id": "s1", "pages": [1]},
            },
            {
                "id": "mc1",
                "type": "multiple_choice",
                "concept_id": "c1",
                "selection_mode": "single",
                "question": "Pick A.",
                "options": [{"id": "a", "text": "A"}, {"id": "b", "text": "B"}],
                "correct_answer_ids": ["a"],
                "hint": "h",
                "correct_feedback": "cf",
                "incorrect_feedback": "if",
            },
        ],
    }


def test_valid_lesson_passes(base_lesson: dict) -> None:
    assert validate_lesson(base_lesson).ok


def test_correct_answer_must_exist(base_lesson: dict) -> None:
    base_lesson["sections"][1]["correct_answer_ids"] = ["z"]
    assert "correct_answer_missing" in _codes(base_lesson)


def test_single_mode_requires_exactly_one_answer(base_lesson: dict) -> None:
    base_lesson["sections"][1]["correct_answer_ids"] = ["a", "b"]
    assert "single_mode_multiple_answers" in _codes(base_lesson)


def test_unknown_concept_detected(base_lesson: dict) -> None:
    base_lesson["sections"][1]["concept_id"] = "ghost"
    assert "unknown_concept" in _codes(base_lesson)


def test_unknown_source_detected(base_lesson: dict) -> None:
    base_lesson["sections"][0]["grounding"]["source_id"] = "ghost"
    assert "unknown_source" in _codes(base_lesson)


def test_duplicate_question_detected(base_lesson: dict) -> None:
    dup = copy.deepcopy(base_lesson["sections"][1])
    dup["id"] = "mc2"
    dup["question"] = "pick a"  # same after normalisation
    base_lesson["sections"].append(dup)
    assert "duplicate_question" in _codes(base_lesson)


def test_duplicate_section_id_detected(base_lesson: dict) -> None:
    dup = copy.deepcopy(base_lesson["sections"][1])
    dup["question"] = "Different question entirely."
    base_lesson["sections"].append(dup)  # same id 'mc1'
    assert "duplicate_section_id" in _codes(base_lesson)


def test_too_many_options_for_age(base_lesson: dict) -> None:
    base_lesson["age"] = 5  # early band allows 3 options
    base_lesson["sections"][1]["options"] = [
        {"id": x, "text": x} for x in ("a", "b", "c", "d")
    ]
    assert "too_many_options" in _codes(base_lesson)


def test_word_bank_required_for_young_children() -> None:
    lesson = {
        "schema_version": "1.0",
        "lesson_id": "l1",
        "title": "T",
        "subject": "S",
        "language": "en",
        "age": 5,
        "difficulty": "beginner",
        "learning_objectives": ["x"],
        "concepts": [{"concept_id": "c1", "name": "C"}],
        "source_references": [],
        "completion_message": "done",
        "sections": [
            {
                "id": "f1",
                "type": "fill_in_the_blank",
                "concept_id": "c1",
                "template": "A {{b1}}.",
                "blanks": [{"id": "b1", "accepted_answers": ["cat"]}],
                "hint": "h",
                "correct_feedback": "cf",
                "incorrect_feedback": "if",
            }
        ],
    }
    assert "word_bank_required" in _codes(lesson)


def test_fill_blank_marker_mismatch() -> None:
    lesson = {
        "schema_version": "1.0",
        "lesson_id": "l1",
        "title": "T",
        "subject": "S",
        "language": "en",
        "age": 10,
        "difficulty": "beginner",
        "learning_objectives": ["x"],
        "concepts": [{"concept_id": "c1", "name": "C"}],
        "source_references": [],
        "completion_message": "done",
        "sections": [
            {
                "id": "f1",
                "type": "fill_in_the_blank",
                "concept_id": "c1",
                "template": "A {{b1}} and {{b2}}.",
                "blanks": [{"id": "b1", "accepted_answers": ["cat"]}],
                "hint": "h",
                "correct_feedback": "cf",
                "incorrect_feedback": "if",
            }
        ],
    }
    assert "blank_mismatch" in _codes(lesson)


def test_drag_order_must_be_permutation() -> None:
    lesson = {
        "schema_version": "1.0",
        "lesson_id": "l1",
        "title": "T",
        "subject": "S",
        "language": "en",
        "age": 10,
        "difficulty": "beginner",
        "learning_objectives": ["x"],
        "concepts": [{"concept_id": "c1", "name": "C"}],
        "source_references": [],
        "completion_message": "done",
        "sections": [
            {
                "id": "d1",
                "type": "drag_into_order",
                "concept_id": "c1",
                "instruction": "Order them.",
                "items": [{"id": "i1", "text": "one"}, {"id": "i2", "text": "two"}],
                "correct_order": ["i1", "i9"],
                "hint": "h",
                "correct_feedback": "cf",
                "incorrect_feedback": "if",
            }
        ],
    }
    assert "order_not_permutation" in _codes(lesson)


def test_match_pairs_ambiguous_text() -> None:
    lesson = {
        "schema_version": "1.0",
        "lesson_id": "l1",
        "title": "T",
        "subject": "S",
        "language": "en",
        "age": 10,
        "difficulty": "beginner",
        "learning_objectives": ["x"],
        "concepts": [{"concept_id": "c1", "name": "C"}],
        "source_references": [],
        "completion_message": "done",
        "sections": [
            {
                "id": "m1",
                "type": "match_pairs",
                "concept_id": "c1",
                "instruction": "Match.",
                "pairs": [
                    {"left": {"id": "l1", "text": "Same"}, "right": {"id": "r1", "text": "X"}},
                    {"left": {"id": "l2", "text": "Same"}, "right": {"id": "r2", "text": "Y"}},
                ],
                "hint": "h",
                "correct_feedback": "cf",
                "incorrect_feedback": "if",
            }
        ],
    }
    assert "duplicate_pair_text" in _codes(lesson)
