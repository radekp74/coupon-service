#!/usr/bin/env python3
"""Validate EMP-010 CI/delivery/observability refinement across lifecycle states."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
REF = ROOT / "docs/project/refinements/EMP-010.md"
SUMMARY = ROOT / "docs/project/refinements/EMP-010-summary.md"
CHECKLIST = ROOT / "docs/project/refinements/EMP-010-review-checklist.md"
BACKLOG = ROOT / "docs/project/backlog.md"
STATUS = ROOT / "docs/project/current-status.md"


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
    review = metadata(text, "Review-Result")

    if state not in {"DRAFT", "ACCEPTED"}:
        errors.append("EMP-010 refinement must be DRAFT or ACCEPTED")
    if status not in {"REFINEMENT", "READY", "IN_PROGRESS", "DONE_AND_VERIFIED"}:
        errors.append("EMP-010 uses unsupported Status")
    if implementation not in {"NOT_STARTED", "IN_PROGRESS", "DONE_AND_VERIFIED"}:
        errors.append("EMP-010 uses unsupported Implementation state")
    if allowed not in {"NO", "YES"}:
        errors.append("EMP-010 must declare Implementation-Allowed YES/NO")

    for key in ("CI-Evidence", "Delivery-Evidence", "Observability-Evidence"):
        value = metadata(text, key)
        if value not in {"NOT_MEASURED", "MEASURED", "MEASURED_AND_VERIFIED", "COMPLETE"}:
            errors.append("EMP-010 %s has unsupported value %r" % (key, value))

    if state == "DRAFT":
        expected = {
            "Status": "REFINEMENT",
            "Implementation": "NOT_STARTED",
            "Implementation-Allowed": "NO",
            "Scope-Frozen": "NO",
            "Zaakceptował": "N/A",
            "Data-Akceptacji": "N/A",
        }
        for key, value in expected.items():
            if metadata(text, key) != value:
                errors.append("DRAFT EMP-010 requires %s=%s" % (key, value))
    elif state == "ACCEPTED":
        if allowed != "YES":
            errors.append("ACCEPTED EMP-010 requires Implementation-Allowed: YES")
        if scope != "YES":
            errors.append("ACCEPTED EMP-010 requires Scope-Frozen: YES")
        if metadata(text, "Zaakceptował") != "Radosław Piątek":
            errors.append("ACCEPTED EMP-010 must record owner acceptance")
        if metadata(text, "Data-Akceptacji") != "2026-08-07":
            errors.append("ACCEPTED EMP-010 must record acceptance date 2026-08-07")
        if status == "READY" and implementation != "NOT_STARTED":
            errors.append("READY EMP-010 must retain Implementation: NOT_STARTED")
        if review not in {"ACCEPTED", "VERIFIED"}:
            errors.append("ACCEPTED EMP-010 requires Review-Result ACCEPTED/VERIFIED")


def validate_content(text: str, errors):
    required_tokens = [
        "ubuntu-24.04", "DOCKER=docker make verify", "permissions: contents: read",
        "persist-credentials: false", "pull_request_target", "pełnego 40-znakowego commit SHA",
        "timeout-minutes: 30", "Testcontainers", "git diff --exit-code", "@sha256:<64 hex>",
        "byte-for-byte reproducibility", "clean working tree", "tracked files", "Python 3.9",
        "make delivery-check", "X-Request-Id", "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$", "requestId",
        "logstash", "micrometer-registry-prometheus", "/actuator/prometheus", "coupon.create",
        "coupon.redemption", "client.ip.resolution", "geolocation.resolution", "geolocation.provider",
        "coupon.redemption.transaction", "raw IP", "userId", "coupon code", "request ID",
        "OpenTelemetry tracing", "Grafana/Prometheus server/dashboard", "CODEX_PROMPT.md",
    ]
    for token in required_tokens:
        require(text, token, errors, "EMP-010 refinement")
    for number in range(1, 32):
        require(text, "**AC-%02d:**" % number, errors, "EMP-010 refinement")
    decisions = re.findall(r"^\d+\. \*\*[^\n]+— \*\*ACCEPT\*\*\.$", text, re.MULTILINE)
    if len(decisions) != 8:
        errors.append("EMP-010 must contain exactly 8 accepted owner decisions ending ACCEPT")

    if metadata(text, "Stan-Refinementu") == "ACCEPTED":
        for token in [
            "Zaakceptował: Radosław Piątek", "Data-Akceptacji: 2026-08-07", "Scope-Frozen: YES",
            "ACCEPTED_READY_FOR_IMPLEMENTATION", "Brak. Osiem decyzji właściciela",
        ]:
            require(text, token, errors, "accepted EMP-010 refinement")


def validate_governance(text: str, errors):
    backlog = read(BACKLOG)
    status_doc = read(STATUS)
    state = metadata(text, "Stan-Refinementu")
    status = metadata(text, "Status")
    if state == "DRAFT":
        if "| EMP-010 | EMP-001 | P1 | REFINEMENT | EMP-010 |" not in backlog:
            errors.append("DRAFT backlog must set EMP-010 to REFINEMENT")
        require(status_doc, "Implementation-Allowed: NO", errors, "current-status")
    else:
        if status == "READY" and "| EMP-010 | EMP-001 | P1 | READY | EMP-010 |" not in backlog:
            errors.append("READY backlog must set EMP-010 to READY")
        require(status_doc, "refinement `ACCEPTED`", errors, "current-status")
        require(status_doc, "Implementation-Allowed: YES", errors, "current-status")


def validate_summary_checklist(text: str, errors):
    for path in (SUMMARY, CHECKLIST):
        if not path.is_file():
            errors.append("Missing file: %s" % path.relative_to(ROOT))
    if SUMMARY.is_file():
        summary = read(SUMMARY)
        for token in ["ubuntu-24.04", "byte-reproducible", "X-Request-Id", "/actuator/prometheus"]:
            require(summary, token, errors, "EMP-010 summary")
        if metadata(text, "Stan-Refinementu") == "ACCEPTED":
            for token in ["zaakceptował", "READY", "Implementation-Allowed: YES", "NOT_MEASURED"]:
                require(summary, token, errors, "accepted EMP-010 summary")
    if CHECKLIST.is_file():
        checklist = read(CHECKLIST)
        for token in ["CODEX_PROMPT", "ACCEPT"]:
            require(checklist, token, errors, "EMP-010 checklist")
        if metadata(text, "Stan-Refinementu") == "ACCEPTED":
            for token in ["owner decisions | PASS", "`ACCEPTED`, `READY`", "Implementation-Allowed: YES"]:
                require(checklist, token, errors, "accepted EMP-010 checklist")


def validate_preimplementation_boundary(text: str, errors):
    status = metadata(text, "Status")
    implementation = metadata(text, "Implementation")
    if implementation != "NOT_STARTED" or status not in {"REFINEMENT", "READY"}:
        return
    prohibited_files = [ROOT / ".github/workflows/ci.yml", ROOT / "scripts/check_emp010.py"]
    for path in prohibited_files:
        if path.exists():
            errors.append("Pre-implementation EMP-010 must not contain: %s" % path.relative_to(ROOT))
    pom = read(ROOT / "pom.xml")
    if "micrometer-registry-prometheus" in pom:
        errors.append("Pre-implementation EMP-010 must not add Prometheus dependency")
    dockerfile = read(ROOT / "Dockerfile")
    if re.search(r"^FROM\s+[^\n]+@sha256:[0-9a-f]{64}", dockerfile, re.MULTILINE):
        errors.append("Pre-implementation EMP-010 must not pin Docker digests yet")
    package_script = read(ROOT / "scripts/package_source.sh")
    if "zipfile" in package_script or "delivery-check" in package_script:
        errors.append("Pre-implementation EMP-010 must not implement deterministic delivery yet")


def validate_hygiene(errors):
    matches = list(ROOT.rglob("CODEX_PROMPT.md"))
    if matches:
        errors.append("CODEX_PROMPT.md is forbidden: %s" % ", ".join(str(p.relative_to(ROOT)) for p in matches))


def main() -> int:
    errors = []
    if not REF.is_file():
        errors.append("Missing docs/project/refinements/EMP-010.md")
        text = ""
    else:
        text = read(REF)
        validate_metadata(text, errors)
        validate_content(text, errors)
        validate_governance(text, errors)
        validate_summary_checklist(text, errors)
        validate_preimplementation_boundary(text, errors)
    validate_hygiene(errors)
    if errors:
        for error in errors:
            print("ERROR: %s" % error, file=sys.stderr)
        return 1
    print("SUCCESS: EMP-010 CI/delivery/observability refinement contract valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
