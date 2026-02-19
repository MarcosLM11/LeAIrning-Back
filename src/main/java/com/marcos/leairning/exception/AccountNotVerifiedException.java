package com.marcos.leairning.exception;

public class AccountNotVerifiedException extends RuntimeException {

    public AccountNotVerifiedException() {
        super("Account not verified");
    }
}
