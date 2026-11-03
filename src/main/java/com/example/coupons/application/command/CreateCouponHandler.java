package com.example.coupons.application.command;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.coupons.application.CommandHandler;
import com.example.coupons.domain.event.DomainEventPublisher;
import com.example.coupons.domain.model.CountryCode;
import com.example.coupons.domain.model.Coupon;
import com.example.coupons.domain.model.CouponCode;
import com.example.coupons.domain.repository.CouponRepository;

/** Creates a coupon and returns its normalized code, which is all the caller needs to look it up. */
@Service
public class CreateCouponHandler implements CommandHandler<CreateCouponCommand, String> {

    private final CouponRepository coupons;
    private final DomainEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public CreateCouponHandler(CouponRepository coupons,
                               DomainEventPublisher eventPublisher,
                               TransactionTemplate transactionTemplate,
                               Clock clock) {
        this.coupons = coupons;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public String handle(CreateCouponCommand command) {
        Coupon coupon = Coupon.create(
                new CouponCode(command.code()),
                command.maxUsages(),
                new CountryCode(command.country()),
                clock.instant());

        // The coupon row and its outbox events commit (or roll back) together: that is the point of the outbox.
        transactionTemplate.executeWithoutResult(status -> {
            coupons.add(coupon);
            coupon.pullEvents().forEach(eventPublisher::publish);
        });
        return coupon.code().value();
    }
}
