"""AI provider adapter interface.

The rest of the backend never imports a vendor SDK directly. It depends only on
this interface, so the AI vendor can be swapped (or a fallback added) without
touching the generation pipeline. Two implementations ship: a deterministic
`FakeProvider` for tests and offline development, and `ClaudeProvider` for real
generation.

Every provider must support *structured generation*: given a JSON Schema, it
returns an object that already conforms to that schema. This is why the pipeline
does not need a "retry on malformed JSON" loop -- guaranteeing valid JSON is the
provider's job, not the caller's.
"""

from __future__ import annotations

import abc
from dataclasses import dataclass, field


@dataclass
class GenerationRequest:
    system: str
    prompt: str
    schema: dict
    # 'small' for extraction-style work, 'strong' for lesson generation. The
    # adapter maps these to concrete model ids so cost routing lives in one place.
    tier: str = "strong"
    max_tokens: int = 4096
    # Content that is identical across many calls (e.g. a source summary reused by
    # every Extend Lesson request) goes here so providers that support prompt
    # caching can mark it cacheable and cut the repeat cost dramatically.
    cacheable_prefix: str | None = None
    # Source images (base64 JPEG, no data: prefix): photos of worksheets, textbook
    # pages, or rendered PDF pages. Vision-capable providers read them as the
    # source material alongside the prompt.
    images: list[str] = field(default_factory=list)


class AIProvider(abc.ABC):
    @abc.abstractmethod
    def generate_structured(self, request: GenerationRequest) -> dict:
        """Return an object conforming to request.schema."""
        raise NotImplementedError
