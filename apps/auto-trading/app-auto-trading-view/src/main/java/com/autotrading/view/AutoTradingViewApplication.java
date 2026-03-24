package com.autotrading.view;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.itcall.base.core",
        "com.itcall.base.view",
        "com.autotrading.view"
})
public class AutoTradingViewApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoTradingViewApplication.class, args);
    }
}
