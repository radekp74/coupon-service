#!/usr/bin/env python3
"""Validate that EMP-004 redemption is represented by executable source and tests."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = {
    "src/main/resources/db/migration/V2__enforce_redemption_user_id.sql": ["ck_coupon_redemptions_user_id_visible_ascii", "^[!-~]{1,128}$"],
    "src/main/java/pl/radoslawpiatek/couponservice/coupon/application/RedeemCouponService.java": ["findSnapshot", "clientIpResolver.resolve", "geoLocationResolver.resolve", "transaction.redeem"],
    "src/main/java/pl/radoslawpiatek/couponservice/coupon/application/TransactionalCouponRedemptionService.java": ["@Transactional", "Isolation.READ_COMMITTED", "findForUpdate", "incrementIfCapacity"],
    "src/main/java/pl/radoslawpiatek/couponservice/coupon/adapters/persistence/JdbcCouponRedemptionRepository.java": ["FOR UPDATE", "uq_coupon_redemptions_coupon_user", "current_uses < max_uses", "RETURNING current_uses"],
    "src/main/java/pl/radoslawpiatek/couponservice/coupon/adapters/web/RedeemCouponRequest.java": ["^[!-~]{1,128}$"],
    "src/test/java/pl/radoslawpiatek/couponservice/coupon/adapters/web/CouponRedemptionApiIT.java": ["COUPON_ALREADY_REDEEMED", "COUPON_EXHAUSTED"],
    "docs/api/openapi.yaml": ["operationId: redeemCoupon", "COUPON_ALREADY_REDEEMED", "^[!-~]{1,128}$"],
}
errors=[]
for name,tokens in REQUIRED.items():
    path=ROOT/name
    if not path.is_file(): errors.append(f"missing {name}"); continue
    text=path.read_text()
    for token in tokens:
        if token not in text: errors.append(f"{name} missing {token}")
if list(ROOT.rglob("CODEX_PROMPT.md")): errors.append("forbidden CODEX_PROMPT.md")
if errors:
    print("\n".join("ERROR: "+e for e in errors), file=sys.stderr); sys.exit(1)
print("SUCCESS: EMP-004 transactional redemption implementation contract valid")
