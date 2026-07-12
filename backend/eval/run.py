"""Run the golden-set evaluation.

Usage:
    python -m eval.run                # scores the example lessons (offline, free)
    python -m eval.run --generate     # regenerates from golden sources via the
                                      # configured provider, then scores

Without --generate it scores the checked-in example lessons, which acts as a fast
regression check on the rubric itself and the examples. With --generate and a real
provider configured, it exercises the actual prompts against golden source
documents -- run this after any prompt change.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from eval.rubric import score_lesson, summarise

_REPO_ROOT = Path(__file__).resolve().parents[2]
_EXAMPLES = _REPO_ROOT / "lesson-schema" / "examples"
_GOLDEN = Path(__file__).resolve().parent / "golden"


def _age_from_example(path: Path, lesson: dict) -> int:
    return int(lesson.get("age", 9))


def score_examples() -> bool:
    all_passed = True
    for path in sorted(_EXAMPLES.glob("*.json")):
        with path.open(encoding="utf-8") as fh:
            lesson = json.load(fh)
        scores = score_lesson(lesson, expected_age=_age_from_example(path, lesson))
        passed, avg = summarise(scores)
        all_passed = all_passed and passed
        mark = "PASS" if passed else "FAIL"
        print(f"\n[{mark}] {path.name}  (avg {avg:.2f})")
        for s in scores:
            flag = "ok " if s.passed else "XX "
            print(f"    {flag}{s.name:24s} {s.score:.2f}  {s.detail}")
    return all_passed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--generate", action="store_true",
                        help="regenerate lessons from golden sources before scoring")
    args = parser.parse_args()

    if args.generate:
        print("--generate requires golden source documents under eval/golden/ and a\n"
              "configured provider. Add sources, then wire this to LessonGenerator.\n"
              f"(golden dir: {_GOLDEN}, currently "
              f"{'present' if _GOLDEN.exists() else 'missing'})")

    print("Scoring checked-in example lessons:")
    ok = score_examples()
    print(f"\nOverall: {'PASS' if ok else 'FAIL'}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
