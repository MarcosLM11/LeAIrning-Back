package com.marcos.documentsservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DocumentsServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(DocumentsServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DocumentsServiceApplication.class, args);
    }

    @Bean
    @Profile("!test")
    ApplicationRunner go(FunctionCatalog catalog) {
        Runnable composedFunction = catalog.lookup(null);
        return args -> {
            if (composedFunction != null) {
                composedFunction.run();
            } else {
                log.warn("No composed function found in the catalog");
            }
        };
    }

}
