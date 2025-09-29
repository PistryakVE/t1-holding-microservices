package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountModels.entity.Transaction;
import org.example.accountModels.enums.TransactionStatus;
import org.example.service.AccountService;
import org.example.service.CardService;
import org.example.service.TransactionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CardService cardService;

    @Value("${app.fraud.max-transactions:10}")
    private int maxTransactions;

    @Value("${app.fraud.time-period-minutes:5}")
    private int timePeriodMinutes;

    @Value("${app.fraud.max-amount:5000000}")
    private double maxAmount;

    public boolean isSuspiciousActivity(String cardId) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(timePeriodMinutes);

        // Проверка количества транзакций
        long transactionCount = transactionService.countTransactionsByCardAndTime(cardId, startTime);

        if (transactionCount >= maxTransactions) {
            log.warn("Suspicious activity detected: {} transactions in {} minutes for card {}",
                    transactionCount, timePeriodMinutes, cardId);
            return true;
        }

        // Дополнительная проверка: большие суммы
        List<Transaction> recentTransactions = transactionService.findByCardId(cardId);
        double totalAmount = recentTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();

        if (totalAmount > maxAmount) {
            log.warn("Suspicious amount detected: {} for card {}", totalAmount, cardId);
            return true;
        }

        return false;
    }

    public void handleSuspiciousActivity(String cardId, String accountId) {
        try {
            // Блокируем счет
            accountService.blockAccount(Long.valueOf(accountId));

            // Блокируем карту
            cardService.blockCard(cardId);

            log.warn("Account {} and card {} blocked due to suspicious activity", accountId, cardId);

        } catch (Exception e) {
            log.error("Error handling suspicious activity for card {}: {}", cardId, e.getMessage());
        }
    }
}