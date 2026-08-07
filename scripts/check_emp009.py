#!/usr/bin/env python3
"""Validate the implemented deterministic concurrency evidence for EMP-009."""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

ROOT = Path(os.environ.get("EMP009_CHECK_ROOT", Path(__file__).resolve().parents[1]))
REDEMPTION = ROOT / "src/test/java/pl/radoslawpiatek/couponservice/coupon/adapters/web/CouponRedemptionApiIT.java"
CREATE = ROOT / "src/test/java/pl/radoslawpiatek/couponservice/coupon/adapters/web/CreateCouponApiIT.java"


def method(source: str, name: str) -> str:
    match = re.search(r"@Test\s+void\s+" + re.escape(name) + r"\(\)", source)
    if not match:
        raise ValueError(f"missing test method {name}")
    start = match.start()
    next_test = re.search(r"\n\s*@Test\s+", source[match.end():])
    end = len(source) if not next_test else match.end() + next_test.start()
    return source[start:end]


def require(section: str, label: str, *tokens: str) -> None:
    for token in tokens:
        if token not in section:
            ERRORS.append(f"{label} missing required evidence: {token}")


ERRORS: list[str] = []
if not REDEMPTION.is_file() or not CREATE.is_file():
    ERRORS.append("missing concurrency integration-test source")
else:
    redemption = REDEMPTION.read_text(encoding="utf-8")
    create = CREATE.read_text(encoding="utf-8")
    if "Thread.sleep" in redemption or "Thread.sleep" in create:
        ERRORS.append("Thread.sleep is forbidden in EMP-009 evidence")
    require(redemption, "redemption test fixture", "@Testcontainers", "PostgreSQLContainer", "@ActiveProfiles(\"test\")")

    try:
        capacity = method(redemption, "concurrentUsersRespectExactCapacityInThreeRounds")
        require(capacity, "100/10", "round < 3", "newFixedThreadPool(100)", "new CountDownLatch(100)",
                "Future<ResponseEntity<Map>>", "COUPON_EXHAUSTED", "couponExhausted", "isEqualTo(90)",
                "otherExpectedBusinessErrors", "unknownOutcomes", "isZero()", "current_uses", "isEqualTo(10)",
                "SELECT COUNT(*) FROM coupon_redemptions", "isEqualTo(10L)",
                "COUNT(DISTINCT r.user_id)")
    except ValueError as error:
        ERRORS.append(str(error))

    try:
        same_user = method(redemption, "sameUserConcurrentRetriesProduceExactlyOneSuccessAndNineteenConflicts")
        require(same_user, "same-user", "for (int index = 0; index < 20; index++)", "isEqualTo(1)",
                "isEqualTo(19)", "COUPON_ALREADY_REDEEMED", "isEqualTo(1L)")
        if "COUPON_EXHAUSTED" in same_user:
            ERRORS.append("same-user evidence must not classify exhausted outcomes as accepted")
    except ValueError as error:
        ERRORS.append(str(error))

    try:
        last_slot = method(redemption, "twoDifferentUsersCompeteForTheLastSlotWithExactOutcomes")
        require(last_slot, "last-slot", "List.of(\"user-A\", \"user-B\")", "HttpStatus.CREATED", "HttpStatus.CONFLICT",
                "COUPON_EXHAUSTED", "COUNT(*) FROM coupon_redemptions r JOIN coupons c", "r.user_id IN ('user-A', 'user-B')",
                "isEqualTo(1L)")
        if "COUPON_ALREADY_REDEEMED" in last_slot:
            ERRORS.append("last-slot evidence must not accept already-redeemed outcomes")
    except ValueError as error:
        ERRORS.append(str(error))

    try:
        no_global_lock = method(redemption, "rowLockOnOneCouponDoesNotGloballySerializeAnotherCoupon")
        require(no_global_lock, "no-global-lock", "LOCKA' FOR UPDATE", "CountDownLatch", "locked.await", "release.await",
                "independentRedemption.get", "HttpStatus.CREATED", "release.countDown(); holder.get")
    except ValueError as error:
        ERRORS.append(str(error))

    require(redemption, "executor cleanup", "shutdownNow()", "awaitTermination(15, TimeUnit.SECONDS)", "future.get(60, TimeUnit.SECONDS)")

    try:
        concurrent_create = method(create, "concurrentCaseVariantsProduceExactlyOneCreatedCoupon")
        require(concurrent_create, "concurrent create", "CONCURRENT_ATTEMPTS", "isEqualTo(1)",
                "CONCURRENT_ATTEMPTS - 1", "unexpectedStatuses", "SELECT COUNT(*) FROM coupons")
    except ValueError as error:
        ERRORS.append(str(error))

if list(ROOT.rglob("CODEX_PROMPT.md")):
    ERRORS.append("forbidden CODEX_PROMPT.md")

if ERRORS:
    print("\n".join(f"ERROR: {error}" for error in ERRORS), file=sys.stderr)
    raise SystemExit(1)
print("SUCCESS: EMP-009 deterministic concurrency evidence implementation contract valid")
