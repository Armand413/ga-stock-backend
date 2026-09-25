package com.ga.gestionstock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionStockApplication {
    public static void main(String[] args) {
        SpringApplication.run(GestionStockApplication.class, args);
    }
}
