#!/usr/bin/env python3
"""Validate the EMP-011 final-review refinement across lifecycle states."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
REF = ROOT / "docs/project/refinements/EMP-011.md"
SUMMARY = ROOT / "docs/project/refinements/EMP-011-summary.md"
CHECKLIST = ROOT / "docs/project/refinements/EMP-011-review-checklist.md"
BACKLOG = ROOT / "docs/project/backlog.md"
STATUS = ROOT / "docs/project/current-status.md"
DECISIONS = ROOT / "docs/project/decision-log.md"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def metadata(text: str, key: str):
    match = re.search(r"^%s:\s*(.+?)\s*$" % re.escape(key), text, re.MULTILINE)
    return match.group(1) if match else None


def require(text: str, token: str, errors, where: str):
    if token not in text:
        errors.append("%s missing %r" % (where, token))


def validate_metadata(text: str, errors):
    state = metadata(text, "Stan-Refinementu")
    status = metadata(text, "Status")
    implementation = metadata(text, "Implementation")
    allowed = metadata(text, "Implementation-Allowed")
    scope = metadata(text, "Scope-Frozen")
    if state not in {"DRAFT", "ACCEPTED"}:
        errors.append("EMP-011 refinement must be DRAFT or ACCEPTED")
    if status not in {"REFINEMENT", "READY", "IN_PROGRESS", "DONE_AND_VERIFIED"}:
        errors.append("EMP-011 uses unsupported Status")
    if implementation not in {"NOT_STARTED", "IN_PROGRESS", "DONE_AND_VERIFIED"}:
        errors.append("EMP-011 uses unsupported Implementation state")
    if metadata(text, "Final-Review-Evidence") not in {"NOT_MEASURED", "MEASURED", "MEASURED_AND_VERIFIED"}:
        errors.append("EMP-011 Final-Review-Evidence has unsupported value")
    if metadata(text, "Public-Repo-Evidence") not in {"NOT_MEASURED", "MEASURED", "MEASURED_AND_VERIFIED"}:
        errors.append("EMP-011 Public-Repo-Evidence has unsupported value")
    if state == "DRAFT":
        expected = {
            "Status": "REFINEMENT",
            "Implementation": "NOT_STARTED",
            "Implementation-Allowed": "NO",
            "Scope-Frozen": "NO",
            "Zaakceptował": "N/A",
            "Data-Akceptacji": "N/A",
            "Review-Result": "DRAFT",
        }
        for key, value in expected.items():
            if metadata(text, key) != value:
                errors.append("DRAFT EMP-011 requires %s=%s" % (key, value))
    elif state == "ACCEPTED":
        expected_fixed = {
            "Implementation-Allowed": "YES",
            "Scope-Frozen": "YES",
            "Zaakceptował": "Radosław Piątek",
            "Data-Akceptacji": "2026-08-07",
        }
        for key, value in expected_fixed.items():
            if metadata(text, key) != value:
                errors.append("ACCEPTED EMP-011 requires %s=%s" % (key, value))
        lifecycle = {
            "READY": ("NOT_STARTED", "ACCEPTED"),
            "IN_PROGRESS": ("IN_PROGRESS", "IN_PROGRESS"),
            "DONE_AND_VERIFIED": ("DONE_AND_VERIFIED", "PASS"),
        }
        if status not in lifecycle:
            errors.append("ACCEPTED EMP-011 has invalid lifecycle status")
        else:
            expected_impl, expected_review = lifecycle[status]
            if implementation != expected_impl:
                errors.append("ACCEPTED EMP-011 status %s requires Implementation=%s" % (status, expected_impl))
            if metadata(text, "Review-Result") != expected_review:
                errors.append("ACCEPTED EMP-011 status %s requires Review-Result=%s" % (status, expected_review))


def validate_content(text: str, errors):
    for token in [
        "F-01", "F-02", "F-03", "F-04", "F-05", "F-06", "F-07", "F-08", "F-09", "F-10",
        "112 unit + 23 integration", "95.76%", "86.39%", "95.06%", "88.21%",
        "api-contract.md", "architecture/overview.md", "testing/test-strategy.md",
        "Risk register", "CHECKSUMS.sha256", "Idempotency-Key", "IPWhois",
        "make verify", "make delivery-check", "make export-source", "GitHub Actions",
        "docs/api/openapi.yaml", "CODEX_PROMPT", "R-030", "R-034",
    ]:
        require(text, token, errors, "EMP-011 refinement")
    for number in range(1, 29):
        require(text, "**AC-%02d:**" % number, errors, "EMP-011 refinement")

    state = metadata(text, "Stan-Refinementu")
    pending = re.findall(r"^\d+\. \*\*[^\n]+— PENDING_OWNER_ACCEPTANCE\.\*\*$", text, re.MULTILINE)
    accepted = re.findall(r"^\d+\. \*\*[^\n]+— ACCEPT\.\*\*$", text, re.MULTILINE)
    if state == "DRAFT" and len(pending) != 8:
        errors.append("DRAFT EMP-011 must contain exactly 8 pending owner decisions")
    if state == "ACCEPTED" and len(accepted) != 8:
        errors.append("ACCEPTED EMP-011 must contain exactly 8 accepted owner decisions")


def validate_governance(text: str, errors):
    backlog = read(BACKLOG)
    status_doc = read(STATUS)
    state = metadata(text, "Stan-Refinementu")
    if state == "DRAFT":
        require(backlog, "| EMP-011 | EMP-001 | P0 | REFINEMENT | EMP-011 |", errors, "backlog")
        require(status_doc, "refinement `DRAFT`", errors, "current-status")
        require(status_doc, "Implementation-Allowed: NO", errors, "current-status")
    else:
        current_status = metadata(text, "Status")
        require(backlog, "| EMP-011 | EMP-001 | P0 | %s | EMP-011 |" % current_status, errors, "backlog")
        require(status_doc, "refinement `ACCEPTED`", errors, "current-status")
        require(status_doc, "Implementation-Allowed: YES", errors, "current-status")

    decisions = read(DECISIONS)
    for number in range(61, 69):
        require(decisions, "| D-%03d |" % number, errors, "decision-log")
        expected = "PROPOSED" if state == "DRAFT" else "ACCEPTED"
        if not re.search(r"\| D-%03d \| 2026-08-07 \| %s \|" % (number, expected), decisions):
            errors.append("D-%03d must be %s for EMP-011 lifecycle state" % (number, expected))


def validate_supporting_docs(text: str, errors):
    for path in (SUMMARY, CHECKLIST):
        if not path.is_file():
            errors.append("Missing %s" % path.relative_to(ROOT))
    if SUMMARY.is_file():
        summary = read(SUMMARY)
        for token in ["README", "api-contract.md", "112 unit + 23 integration"]:
            require(summary, token, errors, "EMP-011 summary")
        if not any(token in summary for token in ["NOT_STARTED", "IN_PROGRESS", "DONE_AND_VERIFIED"]):
            errors.append("EMP-011 summary missing lifecycle state")
    if CHECKLIST.is_file():
        checklist = read(CHECKLIST)
        for token in ["pre-implementation boundary", "owner decisions", "CODEX_PROMPT"]:
            require(checklist, token, errors, "EMP-011 checklist")


def validate_preimplementation_boundary(text: str, errors):
    if metadata(text, "Implementation") != "NOT_STARTED":
        return
    # Known current-state defects must still be present before acceptance/implementation.
    expected_stale = [
        (ROOT / "README.md", "EMP-010:** `IN_PROGRESS`"),
        (ROOT / "README.md", "odświeża `CHECKSUMS.sha256`"),
        (ROOT / "docs/api/api-contract.md", "Endpoint wykorzystania kuponu pozostaje niezaimplementowany."),
        (ROOT / "docs/architecture/overview.md", "Przepływ redemption nadal pozostaje planem"),
        (ROOT / "docs/testing/test-strategy.md", "106 unit + 22 integration"),
        (ROOT / "docs/project/refinements/README.md", "evidence pozostaje `NOT_MEASURED`"),
    ]
    for path, token in expected_stale:
        if token not in read(path):
            errors.append("Pre-implementation EMP-011 unexpectedly changed audited stale marker in %s" % path.relative_to(ROOT))
    if (ROOT / "scripts/check_emp011.py").exists():
        errors.append("Pre-implementation EMP-011 must not contain implementation checker scripts/check_emp011.py")


def validate_hygiene(errors):
    matches = list(ROOT.rglob("CODEX_PROMPT.md"))
    if matches:
        errors.append("CODEX_PROMPT.md is forbidden")


def main() -> int:
    errors = []
    if not REF.is_file():
        errors.append("Missing docs/project/refinements/EMP-011.md")
        text = ""
    else:
        text = read(REF)
        validate_metadata(text, errors)
        validate_content(text, errors)
        validate_governance(text, errors)
        validate_supporting_docs(text, errors)
        validate_preimplementation_boundary(text, errors)
    validate_hygiene(errors)
    if errors:
        for error in errors:
            print("ERROR: %s" % error, file=sys.stderr)
        return 1
    print("SUCCESS: EMP-011 final review refinement contract valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
