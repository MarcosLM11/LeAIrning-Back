package com.marcos.usersservice;

import org.springframework.boot.SpringApplication;

public class TestUsersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(UsersServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
