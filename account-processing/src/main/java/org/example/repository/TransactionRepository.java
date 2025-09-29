package org.example.repository;

import org.example.accountModels.entity.Transaction;
import org.example.accountModels.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountId(Long accountId);
    List<Transaction> findByCardCardId(String cardId);
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByAccountIdAndTimestampBetween(Long accountId, LocalDateTime start, LocalDateTime end);
    List<Transaction> findByAccountIdAndStatus(Long accountId, TransactionStatus status);
    long countByCardCardIdAndTimestampAfter(String cardId, LocalDateTime timestamp);

    @Query("SELECT t FROM Transaction t WHERE t.card.cardId = :cardId AND t.timestamp >= :startTime AND t.status = 'COMPLETED'")
    List<Transaction> findRecentSuccessfulTransactions(@Param("cardId") String cardId, @Param("startTime") LocalDateTime startTime);
}