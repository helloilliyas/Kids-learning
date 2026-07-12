"""Lesson generation service.

Orchestrates the two-stage pipeline and always runs the deterministic validator
before returning. A lesson that fails validation is never returned as success;
the caller (and ultimately the parent) sees the errors instead. This keeps the
guarantee that only structurally-and-semantically valid lessons can reach a child.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from app.prompts import lesson as prompts
from app.providers.base import AIProvider, GenerationRequest
from app.validators.deterministic import validate_lesson
from app.validators.schema import structural_errors

_REPO_ROOT = Path(__file__).resolve().parents[3]
_SCHEMA_PATH = _REPO_ROOT / "lesson-schema" / "lesson.schema.json"


@lru_cache(maxsize=1)
def _lesson_schema() -> dict:
    with _SCHEMA_PATH.open(encoding="utf-8") as fh:
        return json.load(fh)


@dataclass
class GenerationResult:
    lesson: dict | None
    structural_errors: list[str]
    semantic_errors: list[str]

    @property
    def ok(self) -> bool:
        return self.lesson is not None and not self.structural_errors and not self.semantic_errors


@dataclass
class LessonRequest:
    source_text: str
    topic: str
    age: int
    subject: str
    objective: str
    difficulty: str = "beginner"
    language: str = "en"
    num_questions: int = 6


class LessonGenerator:
    def __init__(self, provider: AIProvider) -> None:
        self._provider = provider

    def generate(self, req: LessonRequest) -> GenerationResult:
        # Stage 1: analyse the source and build a teaching plan (cheap tier).
        plan = self._provider.generate_structured(
            GenerationRequest(
                system=prompts.ANALYZE_SYSTEM,
                prompt=prompts.build_analyze_prompt(
                    source_text=req.source_text, topic=req.topic, age=req.age,
                    subject=req.subject, objective=req.objective, language=req.language,
                ),
                schema=_PLAN_SCHEMA,
                tier="small",
            )
        )

        # Stage 2: turn the plan into a full lesson (strong tier).
        lesson = self._provider.generate_structured(
            GenerationRequest(
                system=prompts.LESSON_SYSTEM,
                prompt=prompts.build_lesson_prompt(
                    plan=json.dumps(plan, ensure_ascii=False),
                    topic=req.topic, age=req.age, subject=req.subject,
                    difficulty=req.difficulty, language=req.language,
                    num_questions=req.num_questions, objective=req.objective,
                ),
                schema=_lesson_schema(),
                tier="strong",
                max_tokens=8192,
            )
        )

        return self._validate(lesson)

    def _validate(self, lesson: dict) -> GenerationResult:
        struct = structural_errors(lesson)
        if struct:
            # If it is not even structurally valid, semantic checks may crash; skip them.
            return GenerationResult(lesson=lesson, structural_errors=struct, semantic_errors=[])
        semantic = [str(e) for e in validate_lesson(lesson).errors]
        return GenerationResult(lesson=lesson, structural_errors=[], semantic_errors=semantic)


# Loose schema for the intermediate plan. It is internal (never rendered), so it
# only needs enough shape to make the model organise its analysis usefully.
_PLAN_SCHEMA = {
    "type": "object",
    "additionalProperties": True,
    "required": ["concepts", "misconceptions", "teaching_order", "activity_plan"],
    "properties": {
        "concepts": {"type": "array", "items": {"type": "object"}},
        "misconceptions": {"type": "array", "items": {"type": "string"}},
        "teaching_order": {"type": "array", "items": {"type": "string"}},
        "activity_plan": {"type": "array", "items": {"type": "object"}},
    },
}
