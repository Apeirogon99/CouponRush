package com.apeirogon.rush.domain;

/**
 *  쿠폰
 */
public class Coupon {
    private Long id;
    private String name;
    private int totalQuantity;
    private int issuedQuantity;

    public Coupon() {
        this.id = 0L;
        this.name = "";
        this.totalQuantity = 0;
        this.issuedQuantity = 0;
    }

    public Coupon(Long id, String name, int totalQuantity, int issuedQuantity) {
        this.id = id;
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getIssuedQuantity() {
        return issuedQuantity;
    }

    public void setIssuedQuantity(int issuedQuantity) {
        this.issuedQuantity = issuedQuantity;
    }

    public double getRate() {
        return ((double)getIssuedQuantity() / getTotalQuantity()) * 100.0;
    }
}
