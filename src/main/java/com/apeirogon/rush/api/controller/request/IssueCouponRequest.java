package com.apeirogon.rush.api.controller.request;

import jakarta.validation.constraints.NotNull;

public record IssueCouponRequest(
        @NotNull Long userId,
        Long couponId
) {
}
