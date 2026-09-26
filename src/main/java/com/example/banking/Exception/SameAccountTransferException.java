package com.example.banking.Exception;

public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException(String message) {
        super(message);
        System.out.println("welcome to life of 6.3 guy page");
    }
}