"""FastAPI application.

Thin HTTP surface over the generation and validation services. The backend is
deliberately stateless: it processes an upload, returns lesson JSON, and keeps
nothing. All child data lives on the phone. Uploaded files are processed in a
temporary location and deleted after use.

Authentication is a shared bearer token checked on every non-public route. For a
single-family private app this is sufficient: the token is a backend secret,
embedded only in the private APK, never published. It is NOT an AI API key.
"""

from __future__ import annotations

import os

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

from app.providers.base import AIProvider
from app.providers.fake import FakeProvider
from app.services.generator import LessonGenerator, LessonRequest
from app.validators.deterministic import validate_lesson
from app.validators.schema import schema_version, structural_errors

app = FastAPI(title="Kids Learning Backend", version="0.1.0")


def _provider() -> AIProvider:
    """Select the AI provider. Defaults to the deterministic fake unless a real
    provider is configured, so the service boots and is testable without secrets."""
    if os.environ.get("ANTHROPIC_API_KEY"):
        from app.providers.claude import ClaudeProvider

        return ClaudeProvider()
    return FakeProvider()


def require_token(authorization: str | None = Header(default=None)) -> None:
    expected = os.environ.get("APP_SHARED_TOKEN")
    if not expected:
        # No token configured: allow (local dev). In production the secret is set.
        return
    if authorization != f"Bearer {expected}":
        raise HTTPException(status_code=401, detail="Invalid or missing app token.")


class GenerateBody(BaseModel):
    source_text: str = Field(default="", max_length=100_000)
    topic: str = Field(min_length=1, max_length=200)
    age: int = Field(ge=3, le=18)
    subject: str = Field(min_length=1, max_length=60)
    objective: str = Field(default="", max_length=300)
    difficulty: str = Field(default="beginner")
    language: str = Field(default="en")
    num_questions: int = Field(default=6, ge=1, le=20)
    # Base64 JPEG images (no data: prefix): photos of worksheets/textbook pages or
    # PDF pages rendered by the app. Capped to keep requests and vision costs sane.
    source_images: list[str] = Field(default_factory=list, max_length=8)


class ValidateBody(BaseModel):
    lesson: dict


@app.get("/api/health")
def health() -> dict:
    return {"status": "ok"}


@app.get("/api/schema/version")
def schema_version_endpoint() -> dict:
    return {"schema_version": schema_version()}


@app.post("/api/lessons/generate")
def generate_lesson(body: GenerateBody, _: None = Depends(require_token)) -> dict:
    generator = LessonGenerator(_provider())
    result = generator.generate(
        LessonRequest(
            source_text=body.source_text, topic=body.topic, age=body.age,
            subject=body.subject, objective=body.objective,
            difficulty=body.difficulty, language=body.language,
            num_questions=body.num_questions,
            source_images=body.source_images,
        )
    )
    return {
        "ok": result.ok,
        "lesson": result.lesson,
        "structural_errors": result.structural_errors,
        "semantic_errors": result.semantic_errors,
    }


@app.post("/api/lessons/validate")
def validate_endpoint(body: ValidateBody, _: None = Depends(require_token)) -> dict:
    struct = structural_errors(body.lesson)
    semantic = [] if struct else [str(e) for e in validate_lesson(body.lesson).errors]
    return {
        "ok": not struct and not semantic,
        "structural_errors": struct,
        "semantic_errors": semantic,
    }
