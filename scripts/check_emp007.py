#!/usr/bin/env python3
"""Validate the static OpenAPI, Swagger UI and Javadoc contract for EMP-007."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import List

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src" / "main" / "java" / "pl" / "radoslawpiatek" / "couponservice"

REQUIRED_FILES = [
    ROOT / "docs" / "project" / "refinements" / "EMP-007.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-007-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-007-review-checklist.md",
    ROOT / "docs" / "api" / "openapi.yaml",
    ROOT / "src" / "test" / "java" / "pl" / "radoslawpiatek" / "couponservice" / "OpenApiDocumentationIT.java",
]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def require_tokens(path: Path, tokens: list[str], errors: List[str]) -> None:
    text = read(path)
    for token in tokens:
        if token not in text:
            errors.append(f"{path.relative_to(ROOT)} missing token: {token}")


def validate_refinement(errors: List[str]) -> None:
    require_tokens(
        REQUIRED_FILES[0],
        [
            "Task-ID: EMP-007",
            "Stan-Refinementu: ACCEPTED",
            "Implementation-Allowed: YES",
            "Scope-Frozen: YES",
            "### Blokujące\n\nBrak.",
            "EMP007-AC-14",
            "make verify",
        ],
        errors,
    )


def validate_openapi(errors: List[str]) -> None:
    openapi = ROOT / "docs" / "api" / "openapi.yaml"
    require_tokens(
        openapi,
        [
            "openapi: 3.1.0",
            "/api/v1/coupons:",
            "operationId: createCoupon",
            "polishSpringCampaign:",
            "application/problem+json",
            "COUPON_CODE_CONFLICT",
        ],
        errors,
    )
    if "/api/v1/coupons/{code}/redemptions" in read(openapi):
        errors.append("OpenAPI must not describe redemption before the endpoint exists")


def validate_runtime_wiring(errors: List[str]) -> None:
    pom = read(ROOT / "pom.xml")
    for token in [
        "<springdoc-openapi.version>2.9.0</springdoc-openapi.version>",
        "<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>",
        "<maven-javadoc-plugin.version>3.12.0</maven-javadoc-plugin.version>",
        "<artifactId>maven-javadoc-plugin</artifactId>",
        "<doclint>all</doclint>",
        "<directory>docs/api</directory>",
        "<targetPath>static</targetPath>",
    ]:
        if token not in pom:
            errors.append(f"pom.xml missing EMP-007 token: {token}")

    application = ROOT / "src" / "main" / "resources" / "application.yml"
    require_tokens(
        application,
        [
            "springdoc:",
            "path: /swagger-ui",
            "url: /openapi.yaml",
            "disable-swagger-default-url: true",
            "try-it-out-enabled: true",
        ],
        errors,
    )

    require_tokens(
        ROOT / ".dockerignore",
        ["!docs/api/openapi.yaml"],
        errors,
    )
    require_tokens(
        ROOT / "Dockerfile",
        ["COPY docs/api/openapi.yaml docs/api/openapi.yaml"],
        errors,
    )


def validate_javadocs(errors: List[str]) -> None:
    public_contracts = [
        JAVA_ROOT / "coupon" / "domain" / "CouponCode.java",
        JAVA_ROOT / "coupon" / "domain" / "CountryCode.java",
        JAVA_ROOT / "coupon" / "domain" / "Coupon.java",
        JAVA_ROOT / "coupon" / "application" / "CreateCouponCommand.java",
        JAVA_ROOT / "coupon" / "application" / "CreateCouponUseCase.java",
        JAVA_ROOT / "coupon" / "ports" / "CouponRepository.java",
        JAVA_ROOT / "coupon" / "ports" / "UuidGenerator.java",
        JAVA_ROOT / "coupon" / "adapters" / "web" / "CreateCouponRequest.java",
        JAVA_ROOT / "coupon" / "adapters" / "web" / "CouponResponse.java",
        JAVA_ROOT / "coupon" / "adapters" / "web" / "CouponController.java",
    ]
    for path in public_contracts:
        text = read(path)
        if "/**" not in text:
            errors.append(f"public contract lacks Javadoc: {path.relative_to(ROOT)}")

    require_tokens(
        JAVA_ROOT / "coupon" / "application" / "CreateCouponUseCase.java",
        ["@param command", "@return", "@throws"],
        errors,
    )
    require_tokens(
        JAVA_ROOT / "coupon" / "domain" / "CouponCode.java",
        ["canonical", "@param rawValue", "@return"],
        errors,
    )


def validate_tests(errors: List[str]) -> None:
    require_tokens(
        REQUIRED_FILES[4],
        [
            "@Testcontainers",
            "postgres:18.4-alpine",
            'getForEntity("/openapi.yaml"',
            'getForEntity("/swagger-ui/index.html"',
            '"/v3/api-docs/swagger-config"',
            'containsEntry("url", "/openapi.yaml")',
            'doesNotContain("/api/v1/coupons/{code}/redemptions")',
        ],
        errors,
    )


def validate_project_state(errors: List[str]) -> None:
    backlog = read(ROOT / "docs" / "project" / "backlog.md")
    status = read(ROOT / "docs" / "project" / "current-status.md")
    readme = read(ROOT / "README.md")

    if not any(
        state in backlog
        for state in [
            "| EMP-004 | EMP-001 | P0 | BLOCKED | EMP-001 |",
            "| EMP-004 | EMP-001 | P0 | REFINEMENT | EMP-004 |",
        ]
    ):
        errors.append("backlog must keep EMP-004 blocked or in its own refinement")

    verified = "| EMP-007 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-007 |" in backlog
    candidate = "| EMP-007 | EMP-001 | P0 | IN_PROGRESS | EMP-007 |" in backlog
    if not (candidate or verified):
        errors.append("backlog must expose EMP-007 candidate or verified state")

    if candidate:
        required_status = ["OPENAPI_DOCUMENTATION_CANDIDATE", "PENDING_LOCAL_EMP007_GATE"]
        required_readme = ["0.0.8-emp-007-candidate"]
    else:
        required_status = [
            "OPENAPI_DOCUMENTATION_DONE_AND_VERIFIED",
            "LOCAL_EMP007_GATE_PASS",
            "Javadoc/DocLint policy:** `ACTIVE_AND_VERIFIED`",
        ]
        required_readme = ["EMP-007:** `DONE_AND_VERIFIED`", "OpenAPI/Swagger UI:** `DONE_AND_VERIFIED`"]
        release_history = read(ROOT / "docs" / "project" / "release-history.md")
        for token in ["0.0.9-emp-007-verified", "OpenApiDocumentationIT", "DocLint", "/openapi.yaml"]:
            if token not in release_history:
                errors.append(f"release history missing verified EMP-007 evidence: {token}")

    for token in required_status:
        if token not in status:
            errors.append(f"current status missing EMP-007 state token: {token}")
    for token in required_readme:
        if token not in readme:
            errors.append(f"README missing EMP-007 state token: {token}")

    if verified:
        valid_emp006_states = [
            "| EMP-006 | EMP-001 | P0 | REFINEMENT | EMP-006 |",
            "| EMP-006 | EMP-001 | P0 | READY | EMP-006 |",
            "| EMP-006 | EMP-001 | P0 | IN_PROGRESS | EMP-006 |",
            "| EMP-006 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-006 |",
        ]
        if not any(state in backlog for state in valid_emp006_states):
            errors.append("verified EMP-007 requires EMP-006 to reference its own refinement")

    # Do not require a fixed active task or a future checkpoint: both must evolve.
    for token in []:
        if token not in backlog:
            errors.append(f"unexpected missing token: {token}")


def validate_no_prompt_file(errors: List[str]) -> None:
    prompt_files = sorted(ROOT.rglob("CODEX_PROMPT.md"))
    if prompt_files:
        for path in prompt_files:
            errors.append(f"forbidden Codex prompt file in repository: {path.relative_to(ROOT)}")


def main() -> int:
    errors: List[str] = []
    for path in REQUIRED_FILES:
        if not path.is_file():
            errors.append(f"missing EMP-007 file: {path.relative_to(ROOT)}")

    if not errors:
        validate_refinement(errors)
        validate_openapi(errors)
        validate_runtime_wiring(errors)
        validate_javadocs(errors)
        validate_tests(errors)
        validate_project_state(errors)
        validate_no_prompt_file(errors)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print("SUCCESS: EMP-007 static OpenAPI and Javadoc contract valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
