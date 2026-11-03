package com.example.coupons.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RedemptionRequest(@NotBlank @Size(max = 64) String userId) {
}
