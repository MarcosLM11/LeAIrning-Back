package com.marcos.leairning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LeAIrningBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeAIrningBackApplication.class, args);
    }

}
