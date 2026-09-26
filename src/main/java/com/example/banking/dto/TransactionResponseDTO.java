package com.example.banking.dto;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    private Long id;

    private Long accountId;

    private String type;

    private double amount;

    private LocalDateTime createdDate;
}
