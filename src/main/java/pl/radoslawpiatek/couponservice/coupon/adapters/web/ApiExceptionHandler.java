package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponCodeConflictException;
import pl.radoslawpiatek.couponservice.coupon.domain.InvalidCouponValueException;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponNotFoundException;
import pl.radoslawpiatek.couponservice.coupon.domain.CountryNotAllowedException;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponAlreadyRedeemedException;
import pl.radoslawpiatek.couponservice.coupon.domain.CouponExhaustedException;
import pl.radoslawpiatek.couponservice.geolocation.domain.ClientIpResolutionException;
import pl.radoslawpiatek.couponservice.geolocation.domain.GeolocationUnavailableException;

/**
 * Maps known application failures to the stable Problem Details contract.
 *
 * <p>Technical causes are logged only for unexpected failures and are never
 * returned in the response body.
 */
@RestControllerAdvice
public final class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({
            InvalidCouponValueException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    ResponseEntity<ProblemDetail> invalidRequest(Exception exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "urn:problem:invalid-request",
                "Invalid request",
                "Request validation failed.",
                "INVALID_REQUEST",
                request
        );
    }

    @ExceptionHandler(CouponCodeConflictException.class)
    ResponseEntity<ProblemDetail> couponCodeConflict(
            CouponCodeConflictException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "urn:problem:coupon-code-conflict",
                "Coupon code conflict",
                "A coupon with this code already exists.",
                "COUPON_CODE_CONFLICT",
                request
        );
    }

    @ExceptionHandler(CouponNotFoundException.class)
    ResponseEntity<ProblemDetail> couponNotFound(HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "urn:problem:coupon-not-found", "Coupon not found",
                "The coupon was not found.", "COUPON_NOT_FOUND", request);
    }

    @ExceptionHandler({ClientIpResolutionException.class, GeolocationUnavailableException.class})
    ResponseEntity<ProblemDetail> geolocationUnavailable(HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "urn:problem:geolocation-unavailable", "Geolocation unavailable",
                "The request could not be located.", "GEOLOCATION_UNAVAILABLE", request);
    }

    @ExceptionHandler(CountryNotAllowedException.class)
    ResponseEntity<ProblemDetail> countryNotAllowed(HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "urn:problem:country-not-allowed", "Country not allowed",
                "The coupon is not available in this country.", "COUNTRY_NOT_ALLOWED", request);
    }

    @ExceptionHandler(CouponAlreadyRedeemedException.class)
    ResponseEntity<ProblemDetail> alreadyRedeemed(HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "urn:problem:coupon-already-redeemed", "Coupon already redeemed",
                "The coupon has already been redeemed by this user.", "COUPON_ALREADY_REDEEMED", request);
    }

    @ExceptionHandler(CouponExhaustedException.class)
    ResponseEntity<ProblemDetail> exhausted(HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "urn:problem:coupon-exhausted", "Coupon usage limit reached",
                "The coupon usage limit has been reached.", "COUPON_EXHAUSTED", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> internalError(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected request failure", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "urn:problem:internal-error",
                "Internal server error",
                "The request could not be completed.",
                "INTERNAL_ERROR",
                request
        );
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String type,
            String title,
            String detail,
            String code,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
