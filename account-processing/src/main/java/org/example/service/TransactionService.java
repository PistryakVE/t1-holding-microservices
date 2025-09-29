package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.accountModels.entity.Transaction;
import org.example.accountModels.enums.TransactionStatus;
import org.example.accountModels.enums.TransactionType;
import org.example.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public List<Transaction> findByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> findByCardId(String cardId) {
        return transactionRepository.findByCardCardId(cardId);
    }

    public List<Transaction> findByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    public List<Transaction> findByAccountIdAndPeriod(Long accountId, LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findByAccountIdAndTimestampBetween(accountId, start, end);
    }

    public long countTransactionsByCardAndTime(String cardId, LocalDateTime startTime) {
        return transactionRepository.countByCardCardIdAndTimestampAfter(cardId, startTime);
    }

    public List<Transaction> findFailedTransactionsByAccount(Long accountId) {
        return transactionRepository.findByAccountIdAndStatus(accountId, TransactionStatus.FAILED);
    }

    public void updateTransactionStatus(Long transactionId, TransactionStatus status) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }
}