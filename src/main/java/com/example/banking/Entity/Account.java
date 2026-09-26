package com.example.banking.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account name cannot be blank")
    private String name;

    @PositiveOrZero(message = "Balance cannot be negative")
    private double balance;

    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
