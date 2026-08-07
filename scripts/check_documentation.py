#!/usr/bin/env python3
"""Validate the lightweight documentation governance contract.

The script intentionally uses only Python's standard library and remains
compatible with Python 3.9+.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Dict, List, Tuple

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"

REQUIRED_FILES = [
    ROOT / "README.md",
    ROOT / "CHANGELOG.md",
    ROOT / "PROJECT_RULES.md",
    ROOT / "AUDIT.md",
    ROOT / "docs" / "README.md",
    ROOT / "docs" / "DOCUMENTATION_INDEX.md",
    ROOT / "docs" / "project" / "documentation-governance.md",
    ROOT / "docs" / "project" / "refinement-process.md",
    ROOT / "docs" / "project" / "definition-of-ready-and-done.md",
    ROOT / "docs" / "project" / "backlog.md",
    ROOT / "docs" / "project" / "current-status.md",
    ROOT / "docs" / "project" / "decision-log.md",
    ROOT / "docs" / "project" / "risk-register.md",
    ROOT / "docs" / "project" / "lessons-learned.md",
    ROOT / "docs" / "project" / "release-history.md",
    ROOT / "docs" / "project" / "refinements" / "README.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-001.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-001-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-001-review-checklist.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-003.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-003-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-003-review-checklist.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-004.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-004-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-004-review-checklist.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-006.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-006-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-006-review-checklist.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-007.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-007-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-007-review-checklist.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-008.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-008-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-008-review-checklist.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-009.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-009-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-009-review-checklist.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-010.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-010-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-010-review-checklist.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-011.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-011-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-011-review-checklist.md",
    ROOT / "docs" / "architecture" / "overview.md",
    ROOT / "docs" / "architecture" / "data-model.md",
    ROOT / "docs" / "api" / "api-contract.md",
    ROOT / "docs" / "testing" / "test-strategy.md",
    ROOT / "docs" / "product" / "requirements-traceability.md",
    ROOT / "docs" / "adr" / "README.md",
    ROOT / "docs" / "adr" / "ADR-0001-modular-monolith-and-technology-stack.md",
    ROOT / "docs" / "adr" / "ADR-0002-concurrency-and-redemption-consistency.md",
    ROOT / "docs" / "adr" / "ADR-0003-geolocation-client-ip-and-privacy.md",
]

ALLOWED_STATUSES = {
    "PLANNED",
    "REFINEMENT",
    "READY",
    "IN_PROGRESS",
    "BLOCKED",
    "DONE",
    "DONE_AND_VERIFIED",
}

TASK_RE = re.compile(r"^EMP-\d{3}$")
MARKDOWN_LINK_RE = re.compile(r"\[[^\]]+\]\(([^)]+)\)")
METADATA_RE = re.compile(r"^([A-Za-z-]+):\s*(.+?)\s*$", re.MULTILINE)


class ValidationError(Exception):
    pass


def read(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError as exc:
        raise ValidationError(f"Plik nie jest poprawnym UTF-8: {path}") from exc


def validate_required_files(errors: List[str]) -> None:
    for path in REQUIRED_FILES:
        if not path.is_file():
            errors.append(f"Brak wymaganego pliku: {path.relative_to(ROOT)}")


def parse_markdown_table(path: Path) -> Tuple[List[str], List[List[str]]]:
    lines = read(path).splitlines()
    for index, line in enumerate(lines):
        if line.startswith("| ID |"):
            if index + 1 >= len(lines):
                raise ValidationError(f"Niekompletna tabela: {path}")
            headers = [cell.strip() for cell in line.strip("|").split("|")]
            rows: List[List[str]] = []
            for candidate in lines[index + 2 :]:
                if not candidate.startswith("|"):
                    break
                cells = [cell.strip() for cell in candidate.strip("|").split("|")]
                if len(cells) == len(headers):
                    rows.append(cells)
            return headers, rows
    raise ValidationError(f"Nie znaleziono tabeli backlogu w {path}")


def parse_refinement_metadata(path: Path) -> Dict[str, str]:
    metadata: Dict[str, str] = {}
    for key, value in METADATA_RE.findall(read(path)):
        metadata[key] = value
    return metadata


def validate_backlog(errors: List[str]) -> Dict[str, Dict[str, str]]:
    path = DOCS / "project" / "backlog.md"
    try:
        headers, rows = parse_markdown_table(path)
    except ValidationError as exc:
        errors.append(str(exc))
        return {}

    expected = ["ID", "Parent", "Priority", "Status", "Refinement", "Zadanie", "Docelowy dowód"]
    if headers != expected:
        errors.append(f"Nieoczekiwane kolumny backlogu: {headers}")
        return {}

    tasks: Dict[str, Dict[str, str]] = {}
    for row in rows:
        task = dict(zip(headers, row))
        task_id = task["ID"]
        if not TASK_RE.match(task_id):
            errors.append(f"Niepoprawny Task ID: {task_id}")
            continue
        if task_id in tasks:
            errors.append(f"Duplikat Task ID: {task_id}")
            continue
        if task["Status"] not in ALLOWED_STATUSES:
            errors.append(f"Niepoprawny status {task['Status']} dla {task_id}")
        if not re.fullmatch(r"P[0-2]", task["Priority"]):
            errors.append(f"Niepoprawny priorytet {task['Priority']} dla {task_id}")
        refinement = task["Refinement"]
        if not TASK_RE.match(refinement):
            errors.append(f"Niepoprawne wskazanie refinementu {refinement} dla {task_id}")
        tasks[task_id] = task

    if not tasks:
        errors.append("Backlog nie zawiera zadań")
    return tasks


def validate_refinements(tasks: Dict[str, Dict[str, str]], errors: List[str]) -> None:
    checked: Dict[str, Dict[str, str]] = {}
    for task_id, task in tasks.items():
        refinement_id = task["Refinement"]
        if refinement_id not in checked:
            path = DOCS / "project" / "refinements" / f"{refinement_id}.md"
            if not path.is_file():
                errors.append(f"Brak refinementu {refinement_id} wskazanego przez {task_id}")
                continue
            metadata = parse_refinement_metadata(path)
            checked[refinement_id] = metadata
            if metadata.get("Task-ID") != refinement_id:
                errors.append(f"Refinement {refinement_id} ma niezgodne Task-ID")
            if metadata.get("Stan-Refinementu") not in {"DRAFT", "ACCEPTED"}:
                errors.append(f"Refinement {refinement_id} ma niepoprawny stan")
            if metadata.get("Stan-Refinementu") == "ACCEPTED":
                if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", metadata.get("Data-Akceptacji", "")):
                    errors.append(f"Refinement {refinement_id} nie ma poprawnej daty akceptacji")
                if metadata.get("Implementation-Allowed") != "YES":
                    errors.append(f"Accepted refinement {refinement_id} nie zezwala na implementację")
                text = read(path)
                if re.search(r"\b(TBD|TODO)\b", text):
                    errors.append(f"Accepted refinement {refinement_id} zawiera TBD/TODO")
                if "### Blokujące\n\nBrak." not in text:
                    errors.append(f"Accepted refinement {refinement_id} nie deklaruje braku pytań blokujących")

        if task["Status"] in {"READY", "IN_PROGRESS", "DONE", "DONE_AND_VERIFIED"}:
            metadata = checked.get(refinement_id, {})
            if metadata.get("Stan-Refinementu") != "ACCEPTED":
                errors.append(
                    f"Zadanie {task_id} w stanie {task['Status']} nie ma accepted refinementu"
                )


def validate_current_status(tasks: Dict[str, Dict[str, str]], errors: List[str]) -> None:
    path = DOCS / "project" / "current-status.md"
    text = read(path)
    match = re.search(r"\*\*Active task:\*\* `([^`]+)`", text)
    if not match:
        errors.append("Current status nie zawiera Active task")
        return
    active = match.group(1)
    if active == "awaiting next refinement":
        if not re.search(r"\*\*Data:\*\* \d{4}-\d{2}-\d{2}", text):
            errors.append("Current status nie ma daty ISO")
        return
    task_id_match = re.match(r"(EMP-\d+)(?:\s+—\s+.+)?$", active)
    if task_id_match:
        active = task_id_match.group(1)
    if active not in tasks:
        errors.append(f"Current status wskazuje nieistniejące zadanie: {active}")
    elif tasks[active]["Status"] == "PLANNED" and "refinement" not in match.group(1).lower():
        errors.append(
            f"Planned active task {active} musi być wyłącznie jawnym następnym refinementem"
        )
    elif tasks[active]["Status"] not in {"PLANNED", "REFINEMENT", "READY", "IN_PROGRESS", "BLOCKED"}:
        errors.append(
            f"Active task {active} ma niedozwolony status {tasks[active]['Status']}"
        )
    if not re.search(r"\*\*Data:\*\* \d{4}-\d{2}-\d{2}", text):
        errors.append("Current status nie ma daty ISO")


def normalize_link_target(raw: str) -> str:
    target = raw.strip()
    if target.startswith("<") and target.endswith(">"):
        target = target[1:-1]
    target = target.split("#", 1)[0]
    target = target.split("?", 1)[0]
    return target


def validate_links(errors: List[str]) -> None:
    markdown_files = sorted(ROOT.rglob("*.md"))
    for path in markdown_files:
        text = read(path)
        for raw in MARKDOWN_LINK_RE.findall(text):
            target = normalize_link_target(raw)
            if not target or target.startswith(("http://", "https://", "mailto:", "urn:")):
                continue
            resolved = (path.parent / target).resolve()
            try:
                resolved.relative_to(ROOT.resolve())
            except ValueError:
                errors.append(f"Link wychodzi poza repozytorium: {path.relative_to(ROOT)} -> {raw}")
                continue
            if not resolved.exists():
                errors.append(f"Uszkodzony link: {path.relative_to(ROOT)} -> {raw}")


def validate_documentation_index(errors: List[str]) -> None:
    index_path = DOCS / "DOCUMENTATION_INDEX.md"
    index_text = read(index_path)
    listed = set()
    for raw in MARKDOWN_LINK_RE.findall(index_text):
        target = normalize_link_target(raw)
        if target and not target.startswith(("http://", "https://")):
            resolved = (index_path.parent / target).resolve()
            if resolved.suffix == ".md":
                try:
                    listed.add(resolved.relative_to(DOCS.resolve()).as_posix())
                except ValueError:
                    pass

    actual = {
        path.relative_to(DOCS).as_posix()
        for path in DOCS.rglob("*.md")
        if path != index_path
    }
    missing = sorted(actual - listed)
    stale = sorted(listed - actual)
    for item in missing:
        errors.append(f"Dokument nie jest wymieniony w indeksie: docs/{item}")
    for item in stale:
        errors.append(f"Indeks wskazuje nieistniejący dokument: docs/{item}")


def validate_text_hygiene(errors: List[str]) -> None:
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or path.suffix not in {".md", ".py", ".sh", ".yml", ".yaml", ".json"}:
            continue
        data = path.read_bytes()
        if b"\r\n" in data:
            errors.append(f"CRLF zamiast LF: {path.relative_to(ROOT)}")
        if data and not data.endswith(b"\n"):
            errors.append(f"Brak finalnego newline: {path.relative_to(ROOT)}")
        if path.suffix != ".md":
            for number, line in enumerate(data.splitlines(), start=1):
                if line.rstrip(b" \t") != line:
                    errors.append(f"Trailing whitespace: {path.relative_to(ROOT)}:{number}")


def validate_unique_register_ids(errors: List[str]) -> None:
    checks = [
        (DOCS / "project" / "decision-log.md", re.compile(r"\| (D-\d{3}) \|"), "decision"),
        (DOCS / "project" / "risk-register.md", re.compile(r"\| (R-\d{3}) \|"), "risk"),
    ]
    for path, pattern, label in checks:
        ids = pattern.findall(read(path))
        duplicates = sorted({value for value in ids if ids.count(value) > 1})
        for value in duplicates:
            errors.append(f"Duplikat {label} ID: {value}")
        if not ids:
            errors.append(f"Brak wpisów w rejestrze {label}: {path.relative_to(ROOT)}")


def main() -> int:
    errors: List[str] = []
    validate_required_files(errors)
    tasks = validate_backlog(errors)
    validate_refinements(tasks, errors)
    validate_current_status(tasks, errors)
    validate_links(errors)
    validate_documentation_index(errors)
    validate_text_hygiene(errors)
    validate_unique_register_ids(errors)

    if errors:
        print("FAILED: documentation governance")
        for error in errors:
            print(f"- {error}")
        return 1

    accepted = set(task["Refinement"] for task in tasks.values())
    print(
        "SUCCESS: documentation governance valid "
        f"({len(tasks)} tasks, {len(accepted)} refinement reference, "
        f"{len(list(DOCS.rglob('*.md')))} documentation files)"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
