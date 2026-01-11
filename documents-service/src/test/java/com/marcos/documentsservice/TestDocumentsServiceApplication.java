package com.marcos.documentsservice;

import org.springframework.boot.SpringApplication;

public class TestDocumentsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(DocumentsServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
