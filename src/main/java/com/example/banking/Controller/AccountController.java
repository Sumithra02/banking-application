package com.example.banking.Controller;



import com.example.banking.Entity.Account;
import com.example.banking.Exception.AccountNotFoundException;
import com.example.banking.Service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody Account account) {

        Account savedAccount = accountService.createAccount(account);

        return ResponseEntity.ok(savedAccount);
    }
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {

        return ResponseEntity.ok(accountService.getAllAccounts());
    }
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {

        Account account = accountService.getAccountById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        return ResponseEntity.ok(account);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody Account account) {

        Account updatedAccount = accountService.updateAccount(id, account)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        return ResponseEntity.ok(updatedAccount);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {

        boolean deleted = accountService.deleteAccount(id);

        if (!deleted) {
            throw new AccountNotFoundException("Account not found");
        }

        return ResponseEntity.noContent().build();
    }
    // Deposit amount into account
    @PostMapping("/{id}/deposit")
    public ResponseEntity<Account> deposit(
            @PathVariable Long id,
            @RequestParam double amount) {

        Account updatedAccount = accountService.deposit(id, amount);

        return ResponseEntity.ok(updatedAccount);
    }
    // Withdraw amount from account
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Account> withdraw(
            @PathVariable Long id,
            @RequestParam double amount) {

        Account updatedAccount = accountService.withdraw(id, amount);

        return ResponseEntity.ok(updatedAccount);
    }

}