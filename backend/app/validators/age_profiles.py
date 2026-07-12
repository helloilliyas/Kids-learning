"""Age-profile configuration.

These rules encode how content must adapt to a child's age. They are enforced in
two places, deliberately: they are injected into the generation prompt (so the AI
aims for the right target), and they are checked again by the deterministic
validator (so a prompt that drifts cannot slip past). The validator is the
authority; the prompt is a hint.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class AgeProfile:
    band: str
    min_age: int
    max_age: int
    max_options: int          # most answer options a question may show
    max_sentence_words: int   # target ceiling for a single sentence
    max_content_chars: int    # target ceiling for one explanation card
    requires_word_bank: bool  # youngest learners tap words instead of typing
    allow_free_text: bool     # whether short written answers are appropriate
    reading_support: str      # 'audio_first', 'audio_optional', 'text_first'


_PROFILES: list[AgeProfile] = [
    AgeProfile("early", 3, 6, max_options=3, max_sentence_words=12,
               max_content_chars=300, requires_word_bank=True,
               allow_free_text=False, reading_support="audio_first"),
    AgeProfile("primary", 7, 9, max_options=4, max_sentence_words=18,
               max_content_chars=600, requires_word_bank=False,
               allow_free_text=True, reading_support="audio_optional"),
    AgeProfile("upper", 10, 13, max_options=5, max_sentence_words=25,
               max_content_chars=900, requires_word_bank=False,
               allow_free_text=True, reading_support="text_first"),
    AgeProfile("teen", 14, 18, max_options=6, max_sentence_words=35,
               max_content_chars=1200, requires_word_bank=False,
               allow_free_text=True, reading_support="text_first"),
]


def profile_for_age(age: int) -> AgeProfile:
    for profile in _PROFILES:
        if profile.min_age <= age <= profile.max_age:
            return profile
    # Clamp out-of-range ages to the nearest band rather than failing.
    if age < _PROFILES[0].min_age:
        return _PROFILES[0]
    return _PROFILES[-1]
