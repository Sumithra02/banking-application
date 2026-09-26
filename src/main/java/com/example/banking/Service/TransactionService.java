package com.example.banking.Service;

import com.example.banking.Entity.Account;
import com.example.banking.Entity.Transaction;
import com.example.banking.Repository.AccountRepository;
import com.example.banking.Repository.TransactionRepository;
import com.example.banking.dto.TransactionDTO;
import com.example.banking.dto.TransactionResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }
@Transactional
    public TransactionResponseDTO createTransaction(TransactionDTO dto) {

        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() ->
                        new RuntimeException("Account not found with id: " + dto.getAccountId()));

        if (dto.getAmount() <= 0) {
            throw new RuntimeException("Transaction amount must be greater than zero");
        }

        if ("DEPOSIT".equalsIgnoreCase(dto.getType())) {

            account.setBalance(account.getBalance() + dto.getAmount());

        } else if ("WITHDRAW".equalsIgnoreCase(dto.getType())) {

            if (account.getBalance() < dto.getAmount()) {
                throw new RuntimeException("Insufficient account balance");
            }

            account.setBalance(account.getBalance() - dto.getAmount());

        } else {

            throw new RuntimeException(
                    "Invalid transaction type. Use DEPOSIT or WITHDRAW"
            );
        }

        accountRepository.save(account);

        Transaction transaction = new Transaction();

        transaction.setAccount(account);
        transaction.setType(dto.getType().toUpperCase());
        transaction.setAmount(dto.getAmount());

        Transaction savedTransaction = transactionRepository.save(transaction);

        return convertToResponseDTO(savedTransaction);
    }
    public List<TransactionResponseDTO> getAllTransactions() {

        return transactionRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    public TransactionResponseDTO getTransactionById(Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found with id: " + id));

        return convertToResponseDTO(transaction);
    }

    public void deleteTransaction(Long id) {

        if (!transactionRepository.existsById(id)) {
            throw new RuntimeException("Transaction not found with id: " + id);
        }

        transactionRepository.deleteById(id);
    }

    private TransactionResponseDTO convertToResponseDTO(Transaction transaction) {

        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCreatedDate()
        );
    }
}