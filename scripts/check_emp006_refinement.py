#!/usr/bin/env python3
"""Validate the EMP-006 Client IP and GeoIP refinement contract."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import List

ROOT = Path(__file__).resolve().parents[1]
REFINEMENT = ROOT / "docs" / "project" / "refinements" / "EMP-006.md"
SUMMARY = ROOT / "docs" / "project" / "refinements" / "EMP-006-summary.md"
CHECKLIST = ROOT / "docs" / "project" / "refinements" / "EMP-006-review-checklist.md"

REQUIRED_FILES = [REFINEMENT, SUMMARY, CHECKLIST]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def require_tokens(path: Path, tokens: List[str], errors: List[str]) -> None:
    text = read(path)
    for token in tokens:
        if token not in text:
            errors.append(f"{path.relative_to(ROOT)} missing token: {token}")


def validate_refinement(errors: List[str]) -> None:
    text = read(REFINEMENT)
    require_tokens(
        REFINEMENT,
        [
            "Task-ID: EMP-006",
            "Stan-Refinementu:",
            "Implementation-Allowed:",
            "Scope-Frozen:",
            "ClientIpResolver",
            "GeoLocationResolver",
            "request.getRemoteAddr()",
            "request.getHeaders",
            "więcej niż jedna fizyczna wartość",
            "Forwarded",
            "X-Forwarded-For",
            "Pierwszy adres spoza zaufanych CIDR",
            "max-forwarded-hops: 20",
            "max-header-length: 4096",
            "16 384",
            "Content-Length",
            "16 385",
            "automatyczne redirecty są wyłączone",
            "300–399",
            "bracketed IPv6",
            "IPv4 z portem",
            "boundary proxy",
            "https://ipwho.is",
            "connect-timeout: 500ms",
            "response-timeout: 1s",
            "GEOLOCATION_UNAVAILABLE",
            "raw IP nie jest utrwalany",
            "EMP006-AC-16",
            "EMP006-AC-21",
            "CODEX_PROMPT.md",
        ],
        errors,
    )

    state = re.search(r"^Stan-Refinementu:\s*(\S+)", text, re.MULTILINE)
    allowed = re.search(r"^Implementation-Allowed:\s*(\S+)", text, re.MULTILINE)
    if not state or state.group(1) not in {"DRAFT", "ACCEPTED"}:
        errors.append("EMP-006 refinement must be DRAFT or ACCEPTED")
        return
    if not allowed:
        errors.append("EMP-006 refinement lacks Implementation-Allowed")
        return

    if state.group(1) == "DRAFT":
        if allowed.group(1) != "NO":
            errors.append("DRAFT EMP-006 must not allow implementation")
        for token in [
            "Zaakceptował: brak",
            "Data-Akceptacji: brak",
            "Review-Result: READY_FOR_OWNER_REVIEW",
            "### Blokujące do akceptacji",
        ]:
            if token not in text:
                errors.append(f"DRAFT EMP-006 missing token: {token}")
    else:
        if allowed.group(1) != "YES":
            errors.append("ACCEPTED EMP-006 must allow implementation")
        for token in [
            "Scope-Frozen: YES",
            "Review-Result: PASS",
            "### Blokujące\n\nBrak.",
            "Zaakceptował: Radosław Piątek",
            "ipwho.is jest wymiennym adapterem demonstracyjnym",
            "Wspólny publiczny błąd 503",
            "nie dodaje cache, retry ani fallbacku providera",
            "Błędny `Forwarded` failuje bez fallbacku do XFF",
            "Stub kraju `PL` działa wyłącznie w profilach `local` i `test`",
            "Pierwszy review otrzymał `REJECT`",
            "security amendment",
        ]:
            if token not in text:
                errors.append(f"ACCEPTED EMP-006 missing token: {token}")
        if not re.search(r"^Data-Akceptacji:\s*\d{4}-\d{2}-\d{2}$", text, re.MULTILINE):
            errors.append("ACCEPTED EMP-006 lacks an ISO acceptance date")


def validate_project_state(errors: List[str]) -> None:
    backlog = read(ROOT / "docs" / "project" / "backlog.md")
    current = read(ROOT / "docs" / "project" / "current-status.md")
    refinement = read(REFINEMENT)

    if "| EMP-006 | EMP-001 | P0 |" not in backlog or "| EMP-006 |" not in backlog:
        errors.append("backlog does not contain EMP-006")
    allowed_statuses = {"REFINEMENT", "READY", "IN_PROGRESS", "DONE", "DONE_AND_VERIFIED"}
    if not any(
        f"| EMP-006 | EMP-001 | P0 | {status} | EMP-006 |" in backlog
        for status in allowed_statuses
    ):
        errors.append("EMP-006 backlog row must reference its own refinement")
    if "Stan-Refinementu: DRAFT" in refinement:
        if "| EMP-004 | EMP-001 | P0 | BLOCKED |" not in backlog:
            errors.append("EMP-004 must remain blocked during EMP-006 refinement")
        for token in [
            "Active task:** `EMP-006`",
            "Implementation allowed:** `NO`",
            "DRAFT_READY_FOR_OWNER_REVIEW",
        ]:
            if token not in current:
                errors.append(f"current status missing draft EMP-006 token: {token}")
    else:
        if not any(
            f"| EMP-006 | EMP-001 | P0 | {status} | EMP-006 |" in backlog
            for status in {"READY", "IN_PROGRESS", "DONE", "DONE_AND_VERIFIED"}
        ):
            errors.append("accepted EMP-006 must be READY or in a later allowed state")
        for token in [
            "EMP-006 refinement:** `ACCEPTED`",
            "Implementation allowed:** `YES`",
            "Implementation EMP-006:** `NOT_STARTED`",
        ]:
            if token not in current:
                errors.append(f"current status missing accepted EMP-006 token: {token}")


def validate_security_contract(errors: List[str]) -> None:
    text = read(REFINEMENT)
    forbidden_claims = [
        "publiczny nagłówek pozwalający wymusić kraj jest w zakresie",
        "raw IP jest przechowywany",
        "adapter automatycznie fallbackuje do drugiego publicznego dostawcy",
        "adapter automatycznie ponawia request w ścieżce requestu",
    ]
    for claim in forbidden_claims:
        if claim in text:
            errors.append(f"EMP-006 refinement contains forbidden claim: {claim}")

    openapi = read(ROOT / "docs" / "api" / "openapi.yaml")
    if "/api/v1/coupons/{code}/redemptions" in openapi:
        errors.append("OpenAPI must not describe redemption during EMP-006 refinement")

    migration = read(ROOT / "src" / "main" / "resources" / "db" / "migration" / "V1__create_coupon_tables.sql")
    if re.search(r"\bip_address\b", migration, re.IGNORECASE):
        errors.append("database schema must not persist raw IP")


def validate_no_prompt_file(errors: List[str]) -> None:
    for path in sorted(ROOT.rglob("CODEX_PROMPT.md")):
        errors.append(f"forbidden Codex prompt file in repository: {path.relative_to(ROOT)}")


def main() -> int:
    errors: List[str] = []
    for path in REQUIRED_FILES:
        if not path.is_file():
            errors.append(f"missing EMP-006 refinement file: {path.relative_to(ROOT)}")

    if not errors:
        validate_refinement(errors)
        validate_project_state(errors)
        validate_security_contract(errors)
        validate_no_prompt_file(errors)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print("SUCCESS: EMP-006 Client IP and GeoIP refinement contract valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
