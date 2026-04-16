package com.zdravdom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Zdravdom - Home Healthcare Platform
 *
 * Java 21 Spring Boot 3.x modular monolith
 */
@SpringBootApplication
@EnableAsync
public class ZdravdomApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZdravdomApplication.class, args);
    }
}