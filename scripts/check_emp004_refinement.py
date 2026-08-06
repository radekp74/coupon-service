#!/usr/bin/env python3
"""Validate the EMP-004 transactional redemption refinement contract."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import List

ROOT = Path(__file__).resolve().parents[1]
REFINEMENT = ROOT / "docs/project/refinements/EMP-004.md"
SUMMARY = ROOT / "docs/project/refinements/EMP-004-summary.md"
CHECKLIST = ROOT / "docs/project/refinements/EMP-004-review-checklist.md"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def require(path: Path, tokens: List[str], errors: List[str]) -> None:
    if not path.is_file():
        errors.append(f"missing EMP-004 refinement file: {path.relative_to(ROOT)}")
        return
    text = read(path)
    for token in tokens:
        if token not in text:
            errors.append(f"{path.relative_to(ROOT)} missing token: {token}")


def validate_refinement(errors: List[str]) -> None:
    require(
        REFINEMENT,
        [
            "Task-ID: EMP-004",
            "Stan-Refinementu:",
            "Implementation-Allowed:",
            "POST /api/v1/coupons/{code}/redemptions",
            "^[!-~]{1,128}$",
            "nie jest trimowany ani normalizowany",
            "COUPON_NOT_FOUND",
            "COUNTRY_NOT_ALLOWED",
            "COUPON_ALREADY_REDEEMED",
            "COUPON_EXHAUSTED",
            "GEOLOCATION_UNAVAILABLE",
            "snapshot lookup",
            "Client IP i GeoIP",
            "osobny Spring bean",
            "READ COMMITTED",
            "SELECT ... FOR UPDATE",
            "country → already redeemed → exhausted",
            "uq_coupon_redemptions_coupon_user",
            "current_uses < max_uses",
            "current_uses == count(coupon_redemptions",
            "100 równoległych requestów",
            "dokładnie 10 odpowiedzi `201`",
            "dokładnie 19 odpowiedzi `409 COUPON_ALREADY_REDEEMED`",
            "Thread.sleep",
            "EMP004-AC-20",
            "Historia review i formalna akceptacja",
            "CODEX_PROMPT.md",
        ],
        errors,
    )

    if not REFINEMENT.is_file():
        return
    text = read(REFINEMENT)
    state = re.search(r"^Stan-Refinementu:\s*(\S+)", text, re.MULTILINE)
    allowed = re.search(r"^Implementation-Allowed:\s*(\S+)", text, re.MULTILINE)
    if not state or state.group(1) not in {"DRAFT", "ACCEPTED"}:
        errors.append("EMP-004 refinement must be DRAFT or ACCEPTED")
        return
    if not allowed:
        errors.append("EMP-004 refinement lacks Implementation-Allowed")
        return

    if state.group(1) == "DRAFT":
        if allowed.group(1) != "NO":
            errors.append("DRAFT EMP-004 must not allow implementation")
        for token in [
            "Zaakceptował: brak",
            "Data-Akceptacji: brak",
            "Scope-Frozen: NO",
            "Review-Result: READY_FOR_OWNER_REVIEW",
            "V1 sprawdza dla `user_id` wyłącznie niepustość",
            "Do czasu rozstrzygnięcia wszystkich pięciu punktów",
        ]:
            if token not in text:
                errors.append(f"DRAFT EMP-004 missing token: {token}")
    else:
        if allowed.group(1) != "YES":
            errors.append("ACCEPTED EMP-004 must allow implementation")
        for token in [
            "Scope-Frozen: YES",
            "Review-Result: PASS",
            "Zaakceptował: Radosław Piątek",
            "### Blokujące\n\nBrak.",
            "scalenie EMP-005 z EMP-004",
            "opaque, case-sensitive `userId` `^[!-~]{1,128}$`",
            "Bean Validation/OpenAPI enforcement",
            "PostgreSQL `CHECK` constraintem",
            "ponowienie z tym samym `coupon + userId` zwraca `409 COUPON_ALREADY_REDEEMED`",
            "not found → GeoIP unavailable → wrong country → already redeemed → exhausted",
            "`READ COMMITTED + SELECT FOR UPDATE`",
            "dokładnie 10 odpowiedzi `201`",
            "rollback insertu i incrementu",
        ]:
            if token not in text:
                errors.append(f"ACCEPTED EMP-004 missing token: {token}")
        if not re.search(r"^Data-Akceptacji:\s*\d{4}-\d{2}-\d{2}$", text, re.MULTILINE):
            errors.append("ACCEPTED EMP-004 lacks ISO acceptance date")


def validate_project_state(errors: List[str]) -> None:
    backlog = read(ROOT / "docs/project/backlog.md")
    current = read(ROOT / "docs/project/current-status.md")
    refinement = read(REFINEMENT)

    if "| EMP-004 | EMP-001 | P0 | REFINEMENT | EMP-004 |" not in backlog and not any(
        f"| EMP-004 | EMP-001 | P0 | {status} | EMP-004 |" in backlog
        for status in {"READY", "IN_PROGRESS", "DONE", "DONE_AND_VERIFIED"}
    ):
        errors.append("EMP-004 backlog row must reference its own refinement")

    if "Stan-Refinementu: DRAFT" in refinement:
        for token in [
            "Active task:** `EMP-004 — refinement`",
            "Implementation allowed:** `NO` dla `EMP-004`",
            "DRAFT_READY_FOR_OWNER_REVIEW",
        ]:
            if token not in current:
                errors.append(f"current status missing draft EMP-004 token: {token}")
    else:
        if not any(
            f"| EMP-004 | EMP-001 | P0 | {status} | EMP-004 |" in backlog
            for status in {"READY", "IN_PROGRESS", "DONE", "DONE_AND_VERIFIED"}
        ):
            errors.append("accepted EMP-004 must be READY or later")
        for token in [
            "EMP-004 refinement:** `ACCEPTED`",
            "Implementation allowed:** `YES` dla `EMP-004`",
            "EMP-005:** `MERGED_INTO_EMP-004`",
        ]:
            if token not in current:
                errors.append(f"current status missing accepted EMP-004 token: {token}")
        if not any(
            f"Implementation EMP-004:** `{status}`" in current
            for status in {"NOT_STARTED", "IMPLEMENTED", "IN_PROGRESS", "DONE_AND_VERIFIED"}
        ):
            errors.append("accepted EMP-004 must expose a supported implementation state")
        if "| EMP-005 | EMP-004 | P1 | DONE | EMP-004 |" not in backlog or "MERGED_INTO_EMP-004" not in backlog:
            errors.append("EMP-005 must be documented as merged into EMP-004")


def validate_no_premature_implementation(errors: List[str]) -> None:
    openapi = read(ROOT / "docs/api/openapi.yaml")
    implementation_not_started = "Implementation EMP-004:** `NOT_STARTED`" in read(ROOT / "docs/project/current-status.md")
    if implementation_not_started and "/api/v1/coupons/{code}/redemptions" in openapi:
        errors.append("EMP-004 must not add redemption to canonical OpenAPI before implementation starts")

    controller = read(ROOT / "src/main/java/pl/radoslawpiatek/couponservice/coupon/adapters/web/CouponController.java")
    if implementation_not_started and "redempt" in controller.lower():
        errors.append("EMP-004 must not implement redemption in CouponController before implementation starts")


def main() -> int:
    errors: List[str] = []
    for path in [REFINEMENT, SUMMARY, CHECKLIST]:
        if not path.is_file():
            errors.append(f"missing EMP-004 refinement file: {path.relative_to(ROOT)}")
    if not errors:
        validate_refinement(errors)
        validate_project_state(errors)
        validate_no_premature_implementation(errors)
        if list(ROOT.rglob("CODEX_PROMPT.md")):
            errors.append("repository contains forbidden CODEX_PROMPT.md")

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print("SUCCESS: EMP-004 transactional redemption refinement contract valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
