package com.example.coupons.interfaces.rest;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.coupons.application.command.CreateCouponCommand;
import com.example.coupons.application.command.CreateCouponHandler;
import com.example.coupons.application.command.RedeemCouponCommand;
import com.example.coupons.application.command.IdempotentRedeemCouponHandler;
import com.example.coupons.application.query.CouponView;
import com.example.coupons.application.query.GetCouponHandler;
import com.example.coupons.application.query.GetCouponQuery;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Inbound adapter: POST endpoints dispatch commands, GET endpoints dispatch queries.
 * Business outcomes become HTTP in {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/coupons")
class CouponController {

    private final CreateCouponHandler createCoupon;
    private final IdempotentRedeemCouponHandler redeemCoupon;
    private final GetCouponHandler getCoupon;

    CouponController(CreateCouponHandler createCoupon,
                     IdempotentRedeemCouponHandler redeemCoupon,
                     GetCouponHandler getCoupon) {
        this.createCoupon = createCoupon;
        this.redeemCoupon = redeemCoupon;
        this.getCoupon = getCoupon;
    }

    @PostMapping
    ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
        // The command returns only the key; the representation comes from the query side.
        String code = createCoupon.handle(
                new CreateCouponCommand(request.code(), request.maxUsages(), request.country()));
        CouponView coupon = getCoupon.handle(new GetCouponQuery(code));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(code)
                .toUri();
        return ResponseEntity.created(location).body(CouponResponse.from(coupon));
    }

    @GetMapping("/{code}")
    CouponResponse get(@PathVariable String code) {
        return CouponResponse.from(getCoupon.handle(new GetCouponQuery(code)));
    }

    @PostMapping("/{code}/redemptions")
    ResponseEntity<RedemptionResponse> redeem(@PathVariable String code,
                                              @Valid @RequestBody RedemptionRequest request,
                                              @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                              HttpServletRequest httpRequest) {
        // getRemoteAddr() already reflects X-Forwarded-For when (and only when) the request came through
        // a trusted proxy (server.forward-headers-strategy=native); parsing the header by hand
        // would let any client spoof its country.
        var receipt = redeemCoupon.handle(
                new RedeemCouponCommand(code, request.userId(), httpRequest.getRemoteAddr(), idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(RedemptionResponse.from(receipt));
    }
}
