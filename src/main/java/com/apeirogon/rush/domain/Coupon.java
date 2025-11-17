package com.apeirogon.rush.domain;

/**
 *  쿠폰
 */
public class Coupon {
    private Long id;
    private int totalQuantity;
    private int issuedQuantity;

    public Coupon() {
        this.id = 0L;
        this.totalQuantity = 0;
        this.issuedQuantity = 0;
    }

    public Coupon(Long id, int totalQuantity) {
        this.id = id;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
    }

    public Coupon(Long id, int totalQuantity, int issuedQuantity) {
        this.id = id;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
    }

    public Long getId() {
        return id;
    }

    public boolean isSoldOut() {
        return issuedQuantity >= totalQuantity;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getIssuedQuantity() {
        return issuedQuantity;
    }
}
