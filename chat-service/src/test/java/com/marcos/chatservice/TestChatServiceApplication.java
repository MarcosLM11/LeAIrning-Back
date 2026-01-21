package com.marcos.chatservice;

import org.springframework.boot.SpringApplication;

public class TestChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(ChatServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
