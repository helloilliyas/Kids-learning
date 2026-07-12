"""The example lessons must pass both structural and semantic validation.

These fixtures double as the contract the Android renderer is built against, so
if they ever break, both sides are affected. Keep them green.
"""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from app.validators.deterministic import validate_lesson
from app.validators.schema import structural_errors

_EXAMPLES_DIR = Path(__file__).resolve().parents[2] / "lesson-schema" / "examples"
_EXAMPLES = sorted(_EXAMPLES_DIR.glob("*.json"))


def _load(path: Path) -> dict:
    with path.open(encoding="utf-8") as fh:
        return json.load(fh)


@pytest.mark.parametrize("path", _EXAMPLES, ids=lambda p: p.name)
def test_example_is_structurally_valid(path: Path) -> None:
    errors = structural_errors(_load(path))
    assert errors == [], f"{path.name} failed schema validation:\n" + "\n".join(errors)


@pytest.mark.parametrize("path", _EXAMPLES, ids=lambda p: p.name)
def test_example_is_semantically_valid(path: Path) -> None:
    result = validate_lesson(_load(path))
    assert result.ok, f"{path.name} failed semantic validation:\n" + "\n".join(
        str(e) for e in result.errors
    )


def test_examples_exist() -> None:
    assert _EXAMPLES, "No example lessons found to validate."
