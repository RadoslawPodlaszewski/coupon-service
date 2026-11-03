package com.example.coupons.application;

/**
 * Handles one query: a question that changes nothing. The read side bypasses the domain model
 * and reads a purpose-built projection directly, so it is never shaped by aggregate boundaries.
 */
public interface QueryHandler<Q, R> {

    R handle(Q query);
}
