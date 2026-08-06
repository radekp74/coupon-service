ALTER TABLE coupon_redemptions
    DROP CONSTRAINT ck_coupon_redemptions_user_id_not_blank;

ALTER TABLE coupon_redemptions
    ADD CONSTRAINT ck_coupon_redemptions_user_id_visible_ascii
        CHECK (user_id ~ '^[!-~]{1,128}$');
