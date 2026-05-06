from __future__ import annotations

from typing import Iterable


DEFAULT_SENTENCE_PERIOD = "。"
SENTENCE_END_PUNCTUATION = set("。！？!?.,，、；;：:…")
TRAILING_CLOSERS = set('"\')]}”’」』）】》〉』')


def ensure_sentence_ending(text: str, period: str = DEFAULT_SENTENCE_PERIOD) -> str:
    """Append a sentence-ending mark when a training line has none."""
    value = str(text or "").strip()
    if not value:
        return ""

    insert_at = len(value)
    while insert_at > 0 and value[insert_at - 1] in TRAILING_CLOSERS:
        insert_at -= 1

    if insert_at <= 0:
        return value + period
    if value[insert_at - 1] in SENTENCE_END_PUNCTUATION:
        return value
    return value[:insert_at] + period + value[insert_at:]


def normalize_training_texts(texts: Iterable[str], period: str = DEFAULT_SENTENCE_PERIOD) -> list[str]:
    return [normalized for text in texts if (normalized := ensure_sentence_ending(str(text), period))]
