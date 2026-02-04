package com.marcos.leairning.exception;

public class AccountNotVerifiedException extends RuntimeException {

    public AccountNotVerifiedException(String email) {
        super("Account not verified: " + email);
    }
}