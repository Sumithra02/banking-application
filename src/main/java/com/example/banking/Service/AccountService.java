package com.example.banking.Service;

import com.example.banking.Entity.Account;
import com.example.banking.Exception.InvalidAmountException;
import com.example.banking.Exception.SameAccountTransferException;
import com.example.banking.Repository.AccountRepository;
import com.example.banking.dto.TransferResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import com.example.banking.Exception.AccountNotFoundException;
import com.example.banking.Exception.InsufficientBalanceException;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    public Account createAccount(Account account) {

        account.setCreatedDate(LocalDateTime.now());

        return accountRepository.save(account);
    }
    // Get account by ID
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }
    // Update account
    public Optional<Account> updateAccount(Long id, Account account) {

        Optional<Account> existingAccount = accountRepository.findById(id);

        if (existingAccount.isPresent()) {

            Account existing = existingAccount.get();

            existing.setName(account.getName());
            existing.setBalance(account.getBalance());

            return Optional.of(accountRepository.save(existing));
        }

        return Optional.empty();
    }
    public boolean deleteAccount(Long id) {

        if (accountRepository.existsById(id)) {

            accountRepository.deleteById(id);
            return true;
        }

        return false;
    }
// Deposit amount into account
    public Account deposit(Long id, double amount) {


        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        account.setBalance(account.getBalance() + amount);

        return accountRepository.save(account);
    }
// Withdraw amount from account
    public Account withdraw(Long id, double amount) {

        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (account.getBalance() < amount) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance() - amount);

        return accountRepository.save(account);
    }
// Transfer amount from one account to another
    @Transactional
    public TransferResponse transfer(Long fromId, Long toId, double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be greater than zero");
        }
        if (fromId.equals(toId)) {
            throw new SameAccountTransferException(
                    "Sender and receiver accounts cannot be the same");
        }
        Account sender = accountRepository.findById(fromId)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found"));

        Account receiver = accountRepository.findById(toId)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found"));

        if (sender.getBalance() < amount) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        sender.setBalance(sender.getBalance() - amount);

        receiver.setBalance(receiver.getBalance() + amount);

        accountRepository.save(sender);
        accountRepository.save(receiver);
        return new TransferResponse(
                "Transfer successful",
                fromId,
                toId,
                amount
        );
    }
}