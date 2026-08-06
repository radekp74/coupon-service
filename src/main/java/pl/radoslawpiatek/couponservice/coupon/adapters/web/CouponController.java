package pl.radoslawpiatek.couponservice.coupon.adapters.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.radoslawpiatek.couponservice.coupon.application.CreateCouponCommand;
import pl.radoslawpiatek.couponservice.coupon.application.CreateCouponUseCase;

/** HTTP adapter for coupon operations that are currently implemented. */
@RestController
@RequestMapping("/api/v1/coupons")
public final class CouponController {

    private final CreateCouponUseCase createCouponUseCase;

    /**
     * Connects the HTTP boundary to the create-coupon use case.
     *
     * @param createCouponUseCase transactional operation that owns creation semantics
     */
    public CouponController(CreateCouponUseCase createCouponUseCase) {
        this.createCouponUseCase = createCouponUseCase;
    }

    /**
     * Creates one coupon using the canonical uniqueness rules enforced by PostgreSQL.
     *
     * @param request validated JSON payload
     * @return committed coupon state
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CreateCouponRequest request) {
        return CouponResponse.from(createCouponUseCase.create(new CreateCouponCommand(
                request.code(),
                request.maxUses(),
                request.countryCode()
        )));
    }
}
