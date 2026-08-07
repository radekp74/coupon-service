#!/usr/bin/env python3
"""Validate the final EMP-011 repository-review contract."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def require(text: str, token: str, errors: list[str], where: str) -> None:
    if token not in text:
        errors.append(f"{where} missing {token!r}")


def forbid(text: str, token: str, errors: list[str], where: str) -> None:
    if token in text:
        errors.append(f"{where} contains stale/forbidden marker {token!r}")


def main() -> int:
    errors: list[str] = []

    readme = read("README.md")
    for token in [
        "POST /api/v1/coupons",
        "POST /api/v1/coupons/{code}/redemptions",
        "make verify",
        "make docker-up APP_PORT=18080",
        "http://localhost:18080/swagger-ui",
        "http://localhost:18080/openapi.yaml",
        "112 unit tests",
        "23 integration tests",
        "95.76% LINE / 86.39% BRANCH",
        "95.06% LINE / 88.21% BRANCH",
        "SELECT ... FOR UPDATE",
        "client-asserted opaque identifier",
        "Idempotency-Key",
        "ipwho.is",
        "read-only względem `CHECKSUMS.sha256`",
        "make checksums",
        "Recovery / rollback",
    ]:
        require(readme, token, errors, "README.md")
    for stale in [
        "0.0.22-emp-010-refinement-draft",
        "EMP-010:** `IN_PROGRESS`",
        "odświeża `CHECKSUMS.sha256`",
    ]:
        forbid(readme, stale, errors, "README.md")

    api = read("docs/api/api-contract.md")
    require(api, "POST /api/v1/coupons/{code}/redemptions", errors, "api-contract.md")
    forbid(api, "Endpoint wykorzystania kuponu pozostaje niezaimplementowany", errors, "api-contract.md")
    forbid(api, "Endpoint nie istnieje jeszcze w runtime", errors, "api-contract.md")

    architecture = read("docs/architecture/overview.md")
    require(architecture, "Przepływ wykorzystania kuponu — zweryfikowany EMP-004", errors, "architecture/overview.md")
    forbid(architecture, "Przepływ redemption nadal pozostaje planem", errors, "architecture/overview.md")
    forbid(architecture, "Planowany przepływ wykorzystania kuponu", errors, "architecture/overview.md")

    testing = read("docs/testing/test-strategy.md")
    for token in [
        "112 unit + 23 integration tests",
        "655/684 LINE = 95.76%",
        "292/338 BRANCH = 86.39%",
        "462/486 LINE = 95.06%",
        "247/280 BRANCH = 88.21%",
        "EMP-010 — zweryfikowane CI, delivery i obserwowalność",
    ]:
        require(testing, token, errors, "testing/test-strategy.md")
    forbid(testing, "106 unit + 22 integration", errors, "testing/test-strategy.md")
    forbid(testing, "zaakceptowany plan CI, delivery i obserwowalności", errors, "testing/test-strategy.md")

    registry = read("docs/project/refinements/README.md")
    require(registry, "EMP-010 — CI, delivery hardening i obserwowalność](EMP-010.md) — `ACCEPTED`, `DONE_AND_VERIFIED`", errors, "refinements/README.md")
    require(registry, "EMP-011 — finalny review, README i closeout](EMP-011.md) — `ACCEPTED`, status `IN_PROGRESS`", errors, "refinements/README.md")
    forbid(registry, "evidence pozostaje `NOT_MEASURED`", errors, "refinements/README.md")

    risks = read("docs/project/risk-register.md")
    for rid in ["R-006", "R-022", "R-023", "R-025", "R-027", "R-030", "R-031", "R-032", "R-033", "R-034", "R-014", "R-017"]:
        if not re.search(rf"\| {re.escape(rid)} \| (?:HIGH|MEDIUM) \| MITIGATED \|", risks):
            errors.append(f"risk-register.md does not mark {rid} MITIGATED")
    for rid in ["R-003", "R-009", "R-012", "R-024", "R-026"]:
        if not re.search(rf"\| {re.escape(rid)} \| (?:HIGH|MEDIUM) \| OPEN \|", risks):
            errors.append(f"risk-register.md must keep {rid} OPEN")

    trace = read("docs/product/requirements-traceability.md")
    require(trace, "recruiter-first README", errors, "requirements-traceability.md")
    require(trace, "green GitHub Actions + delivery-check + package hygiene", errors, "requirements-traceability.md")

    ref = read("docs/project/refinements/EMP-011.md")
    for token in [
        "Stan-Refinementu: ACCEPTED",
        "Implementation-Allowed: YES",
        "Scope-Frozen: YES",
        "Status: IN_PROGRESS",
        "Implementation: IN_PROGRESS",
    ]:
        require(ref, token, errors, "EMP-011.md")
    if len(re.findall(r"^\d+\. \*\*[^\n]+— ACCEPT\.\*\*$", ref, re.MULTILINE)) != 8:
        errors.append("EMP-011.md must retain exactly 8 accepted owner decisions")

    # Frozen functional contract: EMP-011 must not introduce additional public API methods.
    openapi = read("docs/api/openapi.yaml")
    public_methods = re.findall(r"^\s{4}(get|post|put|patch|delete):\s*$", openapi, re.MULTILINE)
    if public_methods != ["post", "post"]:
        errors.append(f"canonical OpenAPI must expose exactly two POST operations, got {public_methods}")

    if list(ROOT.rglob("CODEX_PROMPT.md")):
        errors.append("CODEX_PROMPT.md is forbidden")

    if errors:
        print("FAILED: EMP-011 final review contract")
        for error in errors:
            print(f"- {error}")
        return 1
    print("SUCCESS: EMP-011 final review contract valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
