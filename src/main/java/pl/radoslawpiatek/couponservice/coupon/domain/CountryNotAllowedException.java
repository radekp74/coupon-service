package pl.radoslawpiatek.couponservice.coupon.domain;

/** Signals a country-policy mismatch without exposing either country. */
public final class CountryNotAllowedException extends RuntimeException {
    /** Creates the stable policy failure. */
    public CountryNotAllowedException() { super("Coupon is not available in this country."); }
}
