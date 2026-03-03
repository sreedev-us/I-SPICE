package com.company.I_SPICE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ISpiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ISpiceApplication.class, args);
    }
}