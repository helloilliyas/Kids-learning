"""Deterministic provider for tests and offline development.

Produces a small but schema-valid, semantically-consistent lesson without any
network call or API key. It lets the whole pipeline -- generation, validation,
extension -- be exercised in CI for free. It is intentionally simple, not smart:
its output should always pass both validators, so any failure points at a real
bug in the pipeline rather than in a model.
"""

from __future__ import annotations

from app.providers.base import AIProvider, GenerationRequest


class FakeProvider(AIProvider):
    def generate_structured(self, request: GenerationRequest) -> dict:
        schema_id = request.schema.get("$id", "")
        if "lesson" in schema_id or self._looks_like_lesson(request.schema):
            return self._fake_lesson()
        # Fallback: echo an empty object shaped by required keys.
        return {key: None for key in request.schema.get("required", [])}

    @staticmethod
    def _looks_like_lesson(schema: dict) -> bool:
        props = schema.get("properties", {})
        return "sections" in props and "learning_objectives" in props

    @staticmethod
    def _fake_lesson() -> dict:
        return {
            "schema_version": "1.0",
            "lesson_id": "lesson_fake_001",
            "title": "Sample Lesson",
            "subject": "General",
            "language": "en",
            "age": 9,
            "difficulty": "beginner",
            "estimated_duration_minutes": 8,
            "learning_objectives": ["Understand the sample concept"],
            "concepts": [{"concept_id": "sample", "name": "Sample concept"}],
            "source_references": [],
            "sections": [
                {
                    "id": "intro",
                    "type": "explanation",
                    "concept_id": "sample",
                    "difficulty_level": 1,
                    "title": "Introduction",
                    "content": "This is a sample explanation used for testing the pipeline.",
                    "grounding": {"basis": "general_knowledge"},
                },
                {
                    "id": "q1",
                    "type": "multiple_choice",
                    "concept_id": "sample",
                    "difficulty_level": 2,
                    "selection_mode": "single",
                    "question": "Is this a sample question?",
                    "options": [
                        {"id": "a", "text": "Yes"},
                        {"id": "b", "text": "No"},
                    ],
                    "correct_answer_ids": ["a"],
                    "hint": "The lesson title says 'Sample'.",
                    "correct_feedback": "Correct, this is a sample used for testing.",
                    "incorrect_feedback": "Actually it is: everything here is sample data.",
                },
            ],
            "completion_message": "You finished the sample lesson!",
        }
