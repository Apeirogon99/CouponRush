package com.apeirogon.rush;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class CouponRushApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponRushApplication.class, args);
    }

}
