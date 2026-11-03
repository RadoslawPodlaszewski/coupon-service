package com.example.coupons.application.command;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.coupons.application.CommandHandler;
import com.example.coupons.application.port.ClientCountryResolver;
import com.example.coupons.domain.event.DomainEventPublisher;
import com.example.coupons.domain.exception.CouponNotFoundException;
import com.example.coupons.domain.model.CountryCode;
import com.example.coupons.domain.model.Coupon;
import com.example.coupons.domain.model.CouponCode;
import com.example.coupons.domain.model.Redemption;
import com.example.coupons.domain.model.UserId;
import com.example.coupons.domain.repository.CouponRepository;
import com.example.coupons.domain.repository.RedemptionRepository;
import com.example.coupons.domain.service.CouponRedeemer;

/**
 * Not transactional as a whole on purpose: the slow external call (geolocation) runs outside
 * any transaction, so it never holds a database connection or a row lock.
 */
@Service
public class RedeemCouponHandler implements CommandHandler<RedeemCouponCommand, RedemptionReceipt> {

    private final CouponRepository coupons;
    private final RedemptionRepository redemptions;
    private final CouponRedeemer couponRedeemer;
    private final ClientCountryResolver clientCountryResolver;
    private final DomainEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public RedeemCouponHandler(CouponRepository coupons,
                               RedemptionRepository redemptions,
                               CouponRedeemer couponRedeemer,
                               ClientCountryResolver clientCountryResolver,
                               DomainEventPublisher eventPublisher,
                               TransactionTemplate transactionTemplate,
                               Clock clock) {
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.couponRedeemer = couponRedeemer;
        this.clientCountryResolver = clientCountryResolver;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public RedemptionReceipt handle(RedeemCouponCommand command) {
        CouponCode code = new CouponCode(command.code());
        UserId userId = new UserId(command.userId());

        // Cheap check first: don't call the external service for codes that do not exist.
        if (!coupons.existsByCode(code)) {
            throw new CouponNotFoundException(code);
        }

        Optional<CountryCode> clientCountry = clientCountryResolver.resolve(command.clientIp());

        return Objects.requireNonNull(
                transactionTemplate.execute(status -> redeemInTransaction(code, userId, clientCountry)));
    }

    private RedemptionReceipt redeemInTransaction(CouponCode code, UserId userId, Optional<CountryCode> clientCountry) {
        // Any domain exception thrown below rolls the transaction back (and releases the lock).
        Coupon coupon = coupons.findByCodeForUpdate(code)
                .orElseThrow(() -> new CouponNotFoundException(code));

        Redemption redemption = couponRedeemer.redeem(coupon, userId, clientCountry, clock.instant());

        coupons.update(coupon);
        redemptions.save(redemption);
        coupon.pullEvents().forEach(eventPublisher::publish);   // same transaction => transactional outbox
        return new RedemptionReceipt(coupon.code().value(), userId.value(), redemption.redeemedAt());
    }
}
