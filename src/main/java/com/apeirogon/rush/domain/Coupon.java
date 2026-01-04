package com.apeirogon.rush.domain;

/**
 *  쿠폰
 */
public record Coupon(
        Long id,
        int totalQuantity,
        int issuedQuantity
) {

    public Long getId() {
        return id;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getIssuedQuantity() {
        return issuedQuantity;
    }
}
