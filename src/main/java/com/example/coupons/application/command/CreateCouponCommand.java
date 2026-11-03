package com.example.coupons.application.command;

public record CreateCouponCommand(String code, int maxUsages, String country) {
}
