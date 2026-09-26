package com.example.banking.Controller;

import com.example.banking.Service.AccountService;
import com.example.banking.dto.TransferResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TransferController {

    private final AccountService accountService;

    public TransferController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @RequestParam Long fromId,
            @RequestParam Long toId,
            @RequestParam double amount) {

        TransferResponse response = accountService.transfer(fromId, toId, amount);

        return ResponseEntity.ok(response);
    }
}
