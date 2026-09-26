package com.example.banking.dto;



import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransferResponse {

    private String message;
    private Long fromAccount;
    private Long toAccount;
    private double amount;
}
