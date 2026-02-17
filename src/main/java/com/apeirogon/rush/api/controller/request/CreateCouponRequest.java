package com.apeirogon.rush.api.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCouponRequest(
        @NotNull @Positive Integer quantity
) {
}
