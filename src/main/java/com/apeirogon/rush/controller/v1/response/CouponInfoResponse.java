package com.apeirogon.rush.controller.v1.response;

/**
 * 쿠폰 정보 Response
 */
public class CouponInfoResponse {
    private final long id;
    private final String name;
    private final int totalQuantity;
    private final int issuedQuantity;

    public CouponInfoResponse(long id, String name, int totalQuantity, int issuedQuantity) {
        this.id = id;
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
    }

    public long getId() { return id;}

    public String getName() {
        return name;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getIssuedQuantity() {
        return issuedQuantity;
    }
}
