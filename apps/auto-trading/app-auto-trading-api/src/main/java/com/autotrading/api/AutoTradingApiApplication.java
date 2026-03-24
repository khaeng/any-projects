package com.autotrading.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.itcall.base.core",
    "com.itcall.base.api",
    "com.autotrading.api"
})
public class AutoTradingApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoTradingApiApplication.class, args);
    }
}
