package com.apeirogon.rush.api;

import com.apeirogon.rush.api.controller.request.CreateCouponRequest;
import com.apeirogon.rush.api.controller.response.CouponResponse;
import com.apeirogon.rush.support.response.ApiResult;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

public record TestFixture(
        TestRestTemplate client
) {

    public <T> ApiResult<T> get(String url, ParameterizedTypeReference<ApiResult<T>> type) {
        ResponseEntity<ApiResult<T>> response = client.exchange(
                RequestEntity.get(url).build(),
                type
        );
        return response.getBody();
    }

    public <T> ApiResult<T> post(String url, Object body, ParameterizedTypeReference<ApiResult<T>> type) {
        ResponseEntity<ApiResult<T>> response = client.exchange(
                RequestEntity.post(url).body(body),
                type
        );
        return response.getBody();
    }

    public void createCoupon() {
        CreateCouponRequest request = new CreateCouponRequest(100);
        ApiResult<CouponResponse> result = post(
                "/coupons",
                request,
                new ParameterizedTypeReference<>() { }
        );
    }
}
