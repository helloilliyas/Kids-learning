"""JSON Schema loading and structural validation.

Wraps the shared lesson.schema.json (the single source of truth that both the
backend and the Android app build against) so the backend can reject
structurally-invalid lessons before the deterministic semantic checks run.
"""

from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path

from jsonschema import Draft202012Validator

# backend/app/validators/schema.py -> repo root is three parents up.
_REPO_ROOT = Path(__file__).resolve().parents[3]
_SCHEMA_PATH = _REPO_ROOT / "lesson-schema" / "lesson.schema.json"


@lru_cache(maxsize=1)
def _validator() -> Draft202012Validator:
    with _SCHEMA_PATH.open(encoding="utf-8") as fh:
        schema = json.load(fh)
    Draft202012Validator.check_schema(schema)
    return Draft202012Validator(schema)


def schema_version() -> str:
    with _SCHEMA_PATH.open(encoding="utf-8") as fh:
        schema = json.load(fh)
    return schema["properties"]["schema_version"]["const"]


def structural_errors(lesson: dict) -> list[str]:
    """Return human-readable JSON Schema errors, empty if the lesson is valid."""
    errors = sorted(_validator().iter_errors(lesson), key=lambda e: list(e.path))
    out = []
    for err in errors:
        location = "/".join(str(p) for p in err.path) or "(root)"
        out.append(f"{location}: {err.message}")
    return out
