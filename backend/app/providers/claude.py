"""Claude implementation of the AI provider adapter.

Uses the Anthropic Messages API with a forced tool call to guarantee the model
returns an object matching the requested JSON Schema (structured output). Model
selection is routed by tier so cheap extraction work does not pay strong-model
prices, and the optional cacheable prefix is marked with cache_control so repeated
content (source summaries during Extend Lesson) is billed at the cache rate.

The Anthropic SDK is imported lazily so that the rest of the backend -- and the
whole test suite, which uses FakeProvider -- runs without the dependency or an API
key present.
"""

from __future__ import annotations

import os

from app.providers.base import AIProvider, GenerationRequest

# Cheapest capable models per tier. Kept here so cost routing lives in one place.
_MODEL_BY_TIER = {
    "small": os.environ.get("CLAUDE_MODEL_SMALL", "claude-haiku-4-5-20251001"),
    "strong": os.environ.get("CLAUDE_MODEL_STRONG", "claude-sonnet-5"),
}

_TOOL_NAME = "emit_structured_output"


class ClaudeProvider(AIProvider):
    def __init__(self, api_key: str | None = None) -> None:
        self._api_key = api_key or os.environ.get("ANTHROPIC_API_KEY")
        if not self._api_key:
            raise RuntimeError(
                "ANTHROPIC_API_KEY is not set. Provide it as a backend secret; "
                "it must never be shipped in the APK."
            )
        self._client = None  # lazily created

    def _get_client(self):
        if self._client is None:
            from anthropic import Anthropic  # imported lazily

            self._client = Anthropic(api_key=self._api_key)
        return self._client

    def generate_structured(self, request: GenerationRequest) -> dict:
        client = self._get_client()
        model = _MODEL_BY_TIER.get(request.tier, _MODEL_BY_TIER["strong"])

        system_blocks = [{"type": "text", "text": request.system}]
        if request.cacheable_prefix:
            system_blocks.append(
                {
                    "type": "text",
                    "text": request.cacheable_prefix,
                    "cache_control": {"type": "ephemeral"},
                }
            )

        # Source images (photos of worksheets, rendered PDF pages) go ahead of the
        # text prompt so the model reads them as the material being taught from.
        user_content: list[dict] = [
            {
                "type": "image",
                "source": {
                    "type": "base64",
                    "media_type": "image/jpeg",
                    "data": image,
                },
            }
            for image in request.images
        ]
        user_content.append({"type": "text", "text": request.prompt})

        message = client.messages.create(
            model=model,
            max_tokens=request.max_tokens,
            system=system_blocks,
            tools=[
                {
                    "name": _TOOL_NAME,
                    "description": "Return the result as structured data matching the schema.",
                    "input_schema": request.schema,
                }
            ],
            tool_choice={"type": "tool", "name": _TOOL_NAME},
            messages=[{"role": "user", "content": user_content}],
        )

        for block in message.content:
            if getattr(block, "type", None) == "tool_use" and block.name == _TOOL_NAME:
                return block.input
        raise RuntimeError("Claude did not return the expected structured tool call.")
