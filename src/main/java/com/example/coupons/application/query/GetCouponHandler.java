package com.example.coupons.application.query;

import org.springframework.stereotype.Service;

import com.example.coupons.application.QueryHandler;
import com.example.coupons.domain.exception.CouponNotFoundException;
import com.example.coupons.domain.model.CouponCode;

@Service
public class GetCouponHandler implements QueryHandler<GetCouponQuery, CouponView> {

    private final CouponReadModel readModel;

    public GetCouponHandler(CouponReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public CouponView handle(GetCouponQuery query) {
        // CouponCode is reused only to normalize the key the same way the write side does.
        CouponCode code = new CouponCode(query.code());
        return readModel.findByCode(code.value())
                .orElseThrow(() -> new CouponNotFoundException(code));
    }
}
