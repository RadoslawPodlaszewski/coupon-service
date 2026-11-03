package com.example.coupons.interfaces.rest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.coupons.application.command.IdempotencyKeyReusedException;
import com.example.coupons.application.command.InvalidIdempotencyKeyException;
import com.example.coupons.application.command.RequestInProgressException;
import com.example.coupons.application.port.ClientCountryUnavailableException;
import com.example.coupons.domain.exception.CountryNotAllowedException;
import com.example.coupons.domain.exception.CouponAlreadyRedeemedException;
import com.example.coupons.domain.exception.CouponNotFoundException;
import com.example.coupons.domain.exception.DuplicateCouponCodeException;
import com.example.coupons.domain.exception.UsageLimitReachedException;
import com.example.coupons.interfaces.rest.ratelimit.RateLimitExceededException;

/** Maps domain and application errors to RFC 7807 {@code application/problem+json} responses. */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CouponNotFoundException.class)
    ProblemDetail couponNotFound(CouponNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "Coupon does not exist.");
    }

    @ExceptionHandler(CountryNotAllowedException.class)
    ProblemDetail countryNotAllowed(CountryNotAllowedException ex) {
        return problem(HttpStatus.FORBIDDEN, "COUNTRY_NOT_ALLOWED", "Coupon cannot be used from your country.");
    }

    @ExceptionHandler(UsageLimitReachedException.class)
    ProblemDetail limitReached(UsageLimitReachedException ex) {
        return problem(HttpStatus.CONFLICT, "COUPON_LIMIT_REACHED",
                "Coupon has reached its maximum number of usages.");
    }

    @ExceptionHandler(CouponAlreadyRedeemedException.class)
    ProblemDetail alreadyRedeemed(CouponAlreadyRedeemedException ex) {
        return problem(HttpStatus.CONFLICT, "COUPON_ALREADY_REDEEMED", "This user has already used the coupon.");
    }

    @ExceptionHandler(DuplicateCouponCodeException.class)
    ProblemDetail duplicateCode(DuplicateCouponCodeException ex) {
        return problem(HttpStatus.CONFLICT, "COUPON_CODE_ALREADY_EXISTS", "A coupon with this code already exists.");
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    ProblemDetail idempotencyKeyReused(IdempotencyKeyReusedException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED",
                "This Idempotency-Key was already used with a different request.");
    }

    @ExceptionHandler(RequestInProgressException.class)
    ProblemDetail requestInProgress(RequestInProgressException ex) {
        return problem(HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                "A request with this Idempotency-Key is still being processed. Retry shortly.");
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    ProblemDetail invalidIdempotencyKey(InvalidIdempotencyKeyException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ProblemDetail> rateLimited(RateLimitExceededException ex) {
        long seconds = Math.max(1, ex.retryAfter().toSeconds() + (ex.retryAfter().toNanosPart() > 0 ? 1 : 0));
        ProblemDetail body = problem(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                "Too many redemption attempts. Retry in %d s.".formatted(seconds));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(body);
    }

    @ExceptionHandler(ClientCountryUnavailableException.class)
    ProblemDetail geolocationUnavailable(ClientCountryUnavailableException ex) {
        log.warn("Geolocation provider unavailable: {}", ex.getMessage(), ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "GEOLOCATION_UNAVAILABLE",
                "Could not verify your country right now. Try again later.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.merge(error.getField(), String.valueOf(error.getDefaultMessage()),
                        (first, second) -> first + "; " + second));

        ProblemDetail problem = ex.getBody();
        problem.setDetail("Request validation failed.");
        problem.setProperty("reason", "VALIDATION_FAILED");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String reason, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("reason", reason);
        return problem;
    }
}
