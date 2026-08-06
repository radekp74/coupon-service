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

@RestController
@RequestMapping("/api/v1/coupons")
public final class CouponController {

    private final CreateCouponUseCase createCouponUseCase;

    public CouponController(CreateCouponUseCase createCouponUseCase) {
        this.createCouponUseCase = createCouponUseCase;
    }

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
