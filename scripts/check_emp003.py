#!/usr/bin/env python3
"""Validate the static implementation contract for EMP-003."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import List

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src" / "main" / "java" / "pl" / "radoslawpiatek" / "couponservice"
TEST_ROOT = ROOT / "src" / "test" / "java" / "pl" / "radoslawpiatek" / "couponservice"

REQUIRED_FILES = [
    ROOT / "docs" / "project" / "refinements" / "EMP-003.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-003-summary.md",
    ROOT / "docs" / "project" / "refinements" / "EMP-003-review-checklist.md",
    ROOT / "docs" / "api" / "openapi.yaml",
    JAVA_ROOT / "coupon" / "domain" / "CouponCode.java",
    JAVA_ROOT / "coupon" / "domain" / "CountryCode.java",
    JAVA_ROOT / "coupon" / "domain" / "Coupon.java",
    JAVA_ROOT / "coupon" / "domain" / "InvalidCouponValueException.java",
    JAVA_ROOT / "coupon" / "domain" / "CouponCodeConflictException.java",
    JAVA_ROOT / "coupon" / "application" / "CreateCouponCommand.java",
    JAVA_ROOT / "coupon" / "application" / "CreateCouponUseCase.java",
    JAVA_ROOT / "coupon" / "application" / "CreateCouponService.java",
    JAVA_ROOT / "coupon" / "ports" / "CouponRepository.java",
    JAVA_ROOT / "coupon" / "ports" / "UuidGenerator.java",
    JAVA_ROOT / "coupon" / "adapters" / "persistence" / "JdbcCouponRepository.java",
    JAVA_ROOT / "coupon" / "adapters" / "web" / "CreateCouponRequest.java",
    JAVA_ROOT / "coupon" / "adapters" / "web" / "CouponResponse.java",
    JAVA_ROOT / "coupon" / "adapters" / "web" / "CouponController.java",
    JAVA_ROOT / "coupon" / "adapters" / "web" / "ApiExceptionHandler.java",
    JAVA_ROOT / "configuration" / "CoreConfiguration.java",
    TEST_ROOT / "coupon" / "domain" / "CouponCodeTest.java",
    TEST_ROOT / "coupon" / "domain" / "CountryCodeTest.java",
    TEST_ROOT / "coupon" / "application" / "CreateCouponServiceTest.java",
    TEST_ROOT / "coupon" / "adapters" / "web" / "CreateCouponApiIT.java",
]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def require_tokens(path: Path, tokens: list[str], errors: List[str]) -> None:
    text = read(path)
    for token in tokens:
        if token not in text:
            errors.append(f"{path.relative_to(ROOT)} missing token: {token}")


def validate_refinement(errors: List[str]) -> None:
    path = REQUIRED_FILES[0]
    require_tokens(
        path,
        [
            "Task-ID: EMP-003",
            "Stan-Refinementu: ACCEPTED",
            "Implementation-Allowed: YES",
            "Scope-Frozen: YES",
            "### Blokujące\n\nBrak.",
            "EMP003-AC-06",
            "make verify",
        ],
        errors,
    )


def validate_domain_boundaries(errors: List[str]) -> None:
    domain_dir = JAVA_ROOT / "coupon" / "domain"
    for path in sorted(domain_dir.glob("*.java")):
        text = read(path)
        if re.search(r"^import org\.springframework\.", text, re.MULTILINE):
            errors.append(f"domain imports Spring: {path.relative_to(ROOT)}")

    require_tokens(
        domain_dir / "CouponCode.java",
        ["trim()", "toUpperCase(Locale.ROOT)", "[A-Za-z0-9_-]{3,64}"],
        errors,
    )
    require_tokens(
        domain_dir / "CountryCode.java",
        ["Locale.getISOCountries()", "toUpperCase(Locale.ROOT)"],
        errors,
    )
    require_tokens(
        domain_dir / "Coupon.java",
        ["currentUses", "1_000_000", "return new Coupon(id, code, createdAt, maxUses, 0, countryCode)"],
        errors,
    )


def validate_application_and_persistence(errors: List[str]) -> None:
    service = JAVA_ROOT / "coupon" / "application" / "CreateCouponService.java"
    require_tokens(
        service,
        ["@Transactional", "UuidGenerator", "Clock", "couponRepository.insert(coupon)"],
        errors,
    )

    repository = JAVA_ROOT / "coupon" / "adapters" / "persistence" / "JdbcCouponRepository.java"
    text = read(repository)
    for token in [
        "INSERT INTO coupons",
        ":normalizedCode",
        "POSTGRES_UNIQUE_VIOLATION = \"23505\"",
        "CouponCodeConflictException",
        ".param(\"code\"",
    ]:
        if token not in text:
            errors.append(f"JdbcCouponRepository missing token: {token}")
    for forbidden in ["existsByCode", "SELECT COUNT", "SELECT 1", "String.format("]:
        if forbidden in text:
            errors.append(f"JdbcCouponRepository contains forbidden preflight/dynamic SQL token: {forbidden}")


def validate_api(errors: List[str]) -> None:
    controller = JAVA_ROOT / "coupon" / "adapters" / "web" / "CouponController.java"
    require_tokens(
        controller,
        [
            '@RequestMapping("/api/v1/coupons")',
            "@PostMapping",
            "@ResponseStatus(HttpStatus.CREATED)",
            "@Valid @RequestBody",
        ],
        errors,
    )

    handler = JAVA_ROOT / "coupon" / "adapters" / "web" / "ApiExceptionHandler.java"
    require_tokens(
        handler,
        [
            "MediaType.APPLICATION_PROBLEM_JSON",
            '"INVALID_REQUEST"',
            '"COUPON_CODE_CONFLICT"',
            '"INTERNAL_ERROR"',
            '"urn:problem:coupon-code-conflict"',
        ],
        errors,
    )


def validate_tests(errors: List[str]) -> None:
    test = TEST_ROOT / "coupon" / "adapters" / "web" / "CreateCouponApiIT.java"
    require_tokens(
        test,
        [
            "@Testcontainers",
            "org.testcontainers.postgresql.PostgreSQLContainer",
            'postgres:18.4-alpine',
            "concurrentCaseVariantsProduceExactlyOneCreatedCoupon",
            "CountDownLatch",
            "Executors.newFixedThreadPool",
            "CONCURRENT_ATTEMPTS - 1",
            "assertThat(result.created()).isEqualTo(1)",
            "assertThat(persisted).isEqualTo(1L)",
            "executor.shutdownNow()",
        ],
        errors,
    )
    if "Thread.sleep" in read(test):
        errors.append("CreateCouponApiIT must not synchronize with Thread.sleep")



def validate_openapi(errors: List[str]) -> None:
    require_tokens(
        ROOT / "docs" / "api" / "openapi.yaml",
        [
            "openapi: 3.1.0",
            "/api/v1/coupons:",
            "operationId: createCoupon",
            "CreateCouponRequest",
            "CouponResponse",
            "application/problem+json",
            "COUPON_CODE_CONFLICT",
        ],
        errors,
    )

def validate_project_state(errors: List[str]) -> None:
    backlog = read(ROOT / "docs" / "project" / "backlog.md")
    status = read(ROOT / "docs" / "project" / "current-status.md")

    candidate_row = "| EMP-003 | EMP-001 | P0 | IN_PROGRESS | EMP-003 |"
    verified_row = "| EMP-003 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-003 |"

    if candidate_row in backlog:
        for token in [
            "CREATE_COUPON_CANDIDATE",
            "CREATE_COUPON_IMPLEMENTED_PENDING_LOCAL_GATE",
            "PENDING_LOCAL_DOCKER_GATE",
        ]:
            if token not in status:
                errors.append(f"current status missing EMP-003 candidate token: {token}")
    elif verified_row in backlog:
        for token in ["CREATE_COUPON_DONE_AND_VERIFIED", "LOCAL_EMP003_GATE_PASS"]:
            if token not in status:
                errors.append(f"current status missing EMP-003 verified token: {token}")
        readme = read(ROOT / "README.md")
        audit = read(ROOT / "AUDIT.md")
        for token in [
            "0.0.7-emp-003-verified",
            "EMP-003:** `DONE_AND_VERIFIED`",
            "EMP-004 — refinement",
            "Implementation allowed:** `NO`",
            "Weryfikacja runtime:** `PASS`",
        ]:
            if token not in readme:
                errors.append(f"README missing EMP-003 verified public-status token: {token}")
        if "EMP-003 został zamknięty jako `DONE_AND_VERIFIED` dopiero po pełnym lokalnym `make verify`, runtime HTTP i exact-count concurrency test." not in audit:
            errors.append("AUDIT missing EMP-003 verified evidence")
    else:
        errors.append("EMP-003 must be IN_PROGRESS or DONE_AND_VERIFIED with its own refinement")


def main() -> int:
    errors: List[str] = []
    for path in REQUIRED_FILES:
        if not path.is_file():
            errors.append(f"missing EMP-003 file: {path.relative_to(ROOT)}")

    if not errors:
        validate_refinement(errors)
        validate_domain_boundaries(errors)
        validate_application_and_persistence(errors)
        validate_api(errors)
        validate_tests(errors)
        validate_openapi(errors)
        validate_project_state(errors)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print("SUCCESS: EMP-003 static create-coupon contract valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
