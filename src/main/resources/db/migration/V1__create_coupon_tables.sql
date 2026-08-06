CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    normalized_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    max_uses INTEGER NOT NULL,
    current_uses INTEGER NOT NULL DEFAULT 0,
    country_code CHAR(2) NOT NULL,

    CONSTRAINT uq_coupons_normalized_code UNIQUE (normalized_code),
    CONSTRAINT ck_coupons_max_uses_range
        CHECK (max_uses > 0 AND max_uses <= 1000000),
    CONSTRAINT ck_coupons_current_uses_range
        CHECK (current_uses >= 0 AND current_uses <= max_uses),
    CONSTRAINT ck_coupons_code_format
        CHECK (code ~ '^[A-Za-z0-9_-]{3,64}$' AND code = btrim(code)),
    CONSTRAINT ck_coupons_normalized_code_format
        CHECK (normalized_code ~ '^[A-Z0-9_-]{3,64}$'),
    CONSTRAINT ck_coupons_canonicalization
        CHECK (normalized_code = upper(code)),
    CONSTRAINT ck_coupons_country_code_format
        CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE TABLE coupon_redemptions (
    id UUID PRIMARY KEY,
    coupon_id UUID NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    resolved_country_code CHAR(2) NOT NULL,
    redeemed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_coupon_redemptions_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE RESTRICT,
    CONSTRAINT uq_coupon_redemptions_coupon_user
        UNIQUE (coupon_id, user_id),
    CONSTRAINT ck_coupon_redemptions_user_id_not_blank
        CHECK (btrim(user_id) <> ''),
    CONSTRAINT ck_coupon_redemptions_country_code_format
        CHECK (resolved_country_code ~ '^[A-Z]{2}$')
);
