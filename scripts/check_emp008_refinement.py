#!/usr/bin/env python3
"""Validate the documentation-only EMP-008 coverage and quality refinement."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
FILES = {
    "docs/project/refinements/EMP-008.md": [
        "Task-ID: EMP-008", "Stan-Refinementu:", "Implementation-Allowed:",
        "Coverage-Evidence:", "Javadoc-Warning-Baseline: 42", "JaCoCo-Version: 0.8.15",
        "LINE >= 80%", "BRANCH >= 70%", "LINE >=75%", "BRANCH >=65%", "Surefire", "Failsafe", "argLine",
        "target/site/jacoco/index.html", "target/site/jacoco/jacoco.xml",
        "coupon domain", "coupon application", "coupon persistence", "Client IP / GeoIP security-sensitive logic",
        "Testcontainers", "WireMock", "Na początku nie ma żadnych exclusions", "make verify", "manual",
        "missed branches", "new warnings = 0", "<=5", "OUT_OF_SCOPE",
        "EMP008-AC-01", "EMP008-AC-23", "CODEX_PROMPT.md",
    ],
    "docs/project/refinements/EMP-008-summary.md": [
        "ACCEPTED", "42",
    ],
    "docs/project/refinements/EMP-008-review-checklist.md": [
        "ACCEPTED", "Implementation-Allowed: YES", "OUT_OF_SCOPE",
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

refinement_path = ROOT / "docs/project/refinements/EMP-008.md"
refinement = refinement_path.read_text(encoding="utf-8") if refinement_path.is_file() else ""
state = re.search(r"^Stan-Refinementu:\s*(\S+)", refinement, re.MULTILINE)
allowed = re.search(r"^Implementation-Allowed:\s*(\S+)", refinement, re.MULTILINE)
status = re.search(r"^Status:\s*(\S+)", refinement, re.MULTILINE)
coverage = re.search(r"^Coverage-Evidence:\s*(\S+)", refinement, re.MULTILINE)
if not state or state.group(1) not in {"DRAFT", "ACCEPTED"}:
    errors.append("EMP-008 refinement must be DRAFT or ACCEPTED")
elif state.group(1) == "DRAFT" and (not allowed or allowed.group(1) != "NO"):
    errors.append("DRAFT EMP-008 must retain Implementation-Allowed: NO")
elif state.group(1) == "ACCEPTED" and (not allowed or allowed.group(1) != "YES"):
    errors.append("ACCEPTED EMP-008 must require Implementation-Allowed: YES")
if status and status.group(1) not in {"REFINEMENT", "READY", "IN_PROGRESS", "DONE_AND_VERIFIED"}:
    errors.append("EMP-008 uses an unsupported status")
if not coverage or coverage.group(1) not in {"NOT_MEASURED", "MEASURED", "MEASURED_AND_VERIFIED"}:
    errors.append("EMP-008 must declare Coverage-Evidence as NOT_MEASURED, MEASURED or MEASURED_AND_VERIFIED")
if state and state.group(1) == "ACCEPTED":
    accepted_tokens = [
        "Zaakceptował: Radosław Piątek", "Data-Akceptacji: 2026-08-07",
        "org.jacoco:jacoco-maven-plugin:0.8.15", "LINE >= 80%", "BRANCH >= 70%",
        "LINE >=75%", "BRANCH >=65%", "Na początku nie ma żadnych exclusions", "finalny justified budget <=5",
        "new warnings = 0", "DocLint errors zawsze = 0", "OUT_OF_SCOPE",
    ]
    for token in accepted_tokens:
        if token not in refinement:
            errors.append(f"ACCEPTED EMP-008 missing owner decision: {token}")
    for ac in range(1, 24):
        if f"EMP008-AC-{ac:02d}" not in refinement:
            errors.append(f"ACCEPTED EMP-008 missing EMP008-AC-{ac:02d}")

if list(ROOT.rglob("CODEX_PROMPT.md")):
    errors.append("forbidden CODEX_PROMPT.md")
if errors:
    print("\n".join(f"ERROR: {error}" for error in errors), file=sys.stderr)
    raise SystemExit(1)
print("SUCCESS: EMP-008 coverage and quality refinement contract valid")
