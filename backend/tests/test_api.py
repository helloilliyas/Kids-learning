"""End-to-end API tests using the FakeProvider (no API key, no network)."""

from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health() -> None:
    assert client.get("/api/health").json() == {"status": "ok"}


def test_schema_version() -> None:
    assert client.get("/api/schema/version").json() == {"schema_version": "1.0"}


def test_generate_returns_valid_lesson() -> None:
    resp = client.post(
        "/api/lessons/generate",
        json={
            "source_text": "Plants make food using sunlight.",
            "topic": "Photosynthesis",
            "age": 9,
            "subject": "Science",
            "objective": "Understand how plants make food",
        },
    )
    body = resp.json()
    assert body["ok"], body
    assert body["lesson"]["schema_version"] == "1.0"
    assert body["structural_errors"] == []
    assert body["semantic_errors"] == []


def test_validate_rejects_bad_lesson() -> None:
    resp = client.post("/api/lessons/validate", json={"lesson": {"title": "broken"}})
    body = resp.json()
    assert not body["ok"]
    assert body["structural_errors"]


def test_token_enforced_when_configured(monkeypatch) -> None:
    monkeypatch.setenv("APP_SHARED_TOKEN", "secret123")
    unauth = client.post("/api/lessons/validate", json={"lesson": {}})
    assert unauth.status_code == 401
    auth = client.post(
        "/api/lessons/validate",
        json={"lesson": {}},
        headers={"Authorization": "Bearer secret123"},
    )
    assert auth.status_code == 200
