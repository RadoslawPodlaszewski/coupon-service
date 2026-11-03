package com.example.coupons.application;

/**
 * Handles one command: an intention to change state. The write side goes through the domain model
 * (aggregate, invariants, repository ports) and returns at most a minimal result, never a read model.
 */
public interface CommandHandler<C, R> {

    R handle(C command);
}
