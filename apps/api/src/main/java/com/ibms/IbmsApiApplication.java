package com.ibms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the IBMS API application. */
@SpringBootApplication
public class IbmsApiApplication {

    /** Bootstraps the Spring Boot application. */
    public static void main(String[] args) {
        SpringApplication.run(IbmsApiApplication.class, args);
    }
}
