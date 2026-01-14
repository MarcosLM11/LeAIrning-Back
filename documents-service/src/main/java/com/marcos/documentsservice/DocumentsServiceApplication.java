package com.marcos.documentsservice;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DocumentsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentsServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner go(FunctionCatalog catalog) {
        Runnable composedFunction = catalog.lookup(null);
        return args -> {
            composedFunction.run();
        };
    }

}
