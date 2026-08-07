#!/usr/bin/env python3
"""Validate the documentation-only EMP-009 concurrency-evidence refinement."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
FILES = {
    "docs/project/refinements/EMP-009.md": [
        "Task-ID: EMP-009", "Stan-Refinementu:", "Implementation-Allowed:",
        "EMP009-AC-01", "EMP009-AC-15", "Co najmniej 3", "100 różnych userId",
        "19 `COUPON_ALREADY_REDEEMED`", "1 `COUPON_EXHAUSTED`", "row lock coupon-A",
        "Thread.sleep", "Testcontainers", "ExecutorService", "Future", "Evidence-State:",
        "CreateCouponApiIT.concurrentCaseVariantsProduceExactlyOneCreatedCoupon",
        "CouponRedemptionApiIT.concurrentUsersRespectExactCapacityInThreeRounds",
        "CouponRedemptionApiIT.sameUserConcurrentRetriesProduceExactlyOneSuccessAndNineteenConflicts",
        "CouponRedemptionApiIT.twoDifferentUsersCompeteForTheLastSlotWithExactOutcomes",
        "CouponRedemptionApiIT.rowLockOnOneCouponDoesNotGloballySerializeAnotherCoupon",
    ],
    "docs/project/refinements/EMP-009-summary.md": ["EVIDENCE_PARTIAL", "EMP-003", "EMP-004", "EMP-008"],
    "docs/project/refinements/EMP-009-review-checklist.md": ["Historyczna rekomendacja", "REJECT", "trzy exact assertions"],
}

EVIDENCE_SOURCES = {
    "src/test/java/pl/radoslawpiatek/couponservice/coupon/adapters/web/CreateCouponApiIT.java": [
        "CONCURRENT_ATTEMPTS = 24", "concurrentCaseVariantsProduceExactlyOneCreatedCoupon",
        "CountDownLatch", "Future<HttpStatusCode>", "unexpectedStatuses", "awaitTermination",
    ],
    "src/test/java/pl/radoslawpiatek/couponservice/coupon/adapters/web/CouponRedemptionApiIT.java": [
        "concurrentUsersRespectExactCapacityInThreeRounds", "round < 3", "newFixedThreadPool(100)",
        "sameUserConcurrentRetriesProduceExactlyOneSuccessAndNineteenConflicts",
        "twoDifferentUsersCompeteForTheLastSlotWithExactOutcomes",
        "rowLockOnOneCouponDoesNotGloballySerializeAnotherCoupon", "FOR UPDATE",
        "CountDownLatch", "Future<", "awaitTermination",
    ],
}

errors = []
for name, tokens in FILES.items():
    path = ROOT / name
    if not path.is_file():
        errors.append(f"missing {name}")
        continue
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        if token not in text:
            errors.append(f"{name} missing {token}")

for name, tokens in EVIDENCE_SOURCES.items():
    path = ROOT / name
    if not path.is_file():
        errors.append(f"missing evidence source {name}")
        continue
    text = path.read_text(encoding="utf-8")
    for token in tokens:
        if token not in text:
            errors.append(f"{name} missing evidence token {token}")
    if "Thread.sleep" in text:
        errors.append(f"forbidden Thread.sleep in {name}")
    if "PostgreSQLContainer" not in text or "@Testcontainers" not in text:
        errors.append(f"{name} must retain PostgreSQL Testcontainers evidence")

refinement_path = ROOT / "docs/project/refinements/EMP-009.md"
refinement = refinement_path.read_text(encoding="utf-8") if refinement_path.is_file() else ""
state = re.search(r"^Stan-Refinementu:\s*(\S+)", refinement, re.MULTILINE)
allowed = re.search(r"^Implementation-Allowed:\s*(\S+)", refinement, re.MULTILINE)
evidence_state = re.search(r"^Evidence-State:\s*(\S+)", refinement, re.MULTILINE)
task_status = re.search(r"^Status:\s*(\S+)", refinement, re.MULTILINE)
implementation = re.search(r"^Implementation:\s*(\S+)", refinement, re.MULTILINE)
if not state or state.group(1) not in {"DRAFT", "ACCEPTED"}:
    errors.append("EMP-009 refinement must be DRAFT or ACCEPTED")
elif state.group(1) == "DRAFT" and (not allowed or allowed.group(1) != "NO"):
    errors.append("DRAFT EMP-009 must retain Implementation-Allowed: NO")
elif state.group(1) == "ACCEPTED":
    if not evidence_state or evidence_state.group(1) not in {"PARTIAL", "COMPLETE"}:
        errors.append("ACCEPTED EMP-009 must expose Evidence-State PARTIAL or COMPLETE")
    for token in [
        "Implementation-Allowed: YES",
        "Zaakceptował: Radosław Piątek", "Data-Akceptacji: 2026-08-07",
        "Zamrożony zakres przyszłej implementacji", "wszystkie 90 konfliktów 100/10",
        "10 unikalnych userId", "dokładnie jednego z dwóch konkurujących userId",
        "scripts/check_emp009.py", "Zakazane są nowe scenariusze biznesowe",
        "JaCoCo", "OpenAPI", "EMP-008",
    ]:
        if token not in refinement:
            errors.append(f"ACCEPTED EMP-009 missing {token}")
    if not task_status or task_status.group(1) not in {"READY", "IN_PROGRESS", "DONE_AND_VERIFIED"}:
        errors.append("ACCEPTED EMP-009 must use READY, IN_PROGRESS or DONE_AND_VERIFIED status")
    if not implementation or implementation.group(1) not in {"NOT_STARTED", "IN_PROGRESS", "DONE_AND_VERIFIED"}:
        errors.append("ACCEPTED EMP-009 must expose an allowed implementation state")
    if task_status and task_status.group(1) == "DONE_AND_VERIFIED":
        for token in [
            "Implementation: DONE_AND_VERIFIED",
            "Evidence-State: COMPLETE",
            "scripts/check_emp009.py",
            "pełny Maven/Testcontainers i Docker gate",
        ]:
            if token not in refinement:
                errors.append(f"verified EMP-009 missing {token}")
        if not (ROOT / "scripts/check_emp009.py").is_file():
            errors.append("verified EMP-009 requires scripts/check_emp009.py")

backlog = (ROOT / "docs/project/backlog.md").read_text(encoding="utf-8")
current = (ROOT / "docs/project/current-status.md").read_text(encoding="utf-8")
if not any(f"| EMP-009 | EMP-001 | P0 | {status} | EMP-009 |" in backlog for status in {"REFINEMENT", "READY", "IN_PROGRESS", "DONE_AND_VERIFIED"}):
    errors.append("EMP-009 backlog must reference its own refinement in an allowed status")
if state and state.group(1) == "DRAFT" and "Active task:** `EMP-009 — refinement deterministycznego concurrency evidence`" not in current:
    errors.append("draft current status must identify EMP-009 refinement as active")
if "EMP-008" not in backlog:
    errors.append("EMP-008 must remain present in the backlog")
if list(ROOT.rglob("CODEX_PROMPT.md")):
    errors.append("forbidden CODEX_PROMPT.md")
if errors:
    print("\n".join("ERROR: " + error for error in errors), file=sys.stderr)
    raise SystemExit(1)
print("SUCCESS: EMP-009 deterministic concurrency evidence refinement contract valid")
