package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.TransactionMessageDto;
import org.example.dto.TransactionProcessingResultDto;
import org.example.accountModels.entity.Account;
import org.example.accountModels.entity.Card;
import org.example.accountModels.entity.Payment;
import org.example.accountModels.entity.Transaction;
import org.example.accountModels.enums.AccountStatus;
import org.example.accountModels.enums.PaymentStatus;
import org.example.accountModels.enums.PaymentType;
import org.example.accountModels.enums.TransactionStatus;
import org.example.accountModels.enums.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessingService {
    private final AccountService accountService;
    private final CardService cardService;
    private final TransactionService transactionService;
    private final PaymentService paymentService;
    private final FraudDetectionService fraudDetectionService;

    @Value("${app.fraud.max-transactions:10}")
    private int maxTransactionsPerPeriod;

    @Value("${app.fraud.time-period-minutes:5}")
    private int fraudTimePeriodMinutes;

    @Transactional
    public TransactionProcessingResultDto processTransaction(TransactionMessageDto message) {
        try {
            // a. Проверка на мошенничество
            if (fraudDetectionService.isSuspiciousActivity(message.getCardId())) {
                return blockAccountAndCard(message);
            }

            // Поиск счета и карты
            Account account = accountService.findById(message.getAccountId());
            Card card = cardService.findByCardId(message.getCardId());

            // b. Проверка статуса счета
            if (account.getStatus() == AccountStatus.BLOCKED ||
                    account.getStatus() == AccountStatus.ARRESTED) {
                return createFailedResult(message, "Account is blocked or arrested");
            }

            // Обработка транзакции
            Transaction transaction = createTransaction(message, account, card);

            if (message.getType() == TransactionType.CREDIT) {
                return processCreditTransaction(message, account, transaction);
            } else {
                return processDebitTransaction(message, account, transaction);
            }

        } catch (Exception e) {
            log.error("Error processing transaction: {}", message.getMessageId(), e);
            return createFailedResult(message, "Processing error: " + e.getMessage());
        }
    }

    private TransactionProcessingResultDto processCreditTransaction(TransactionMessageDto message,
                                                                    Account account,
                                                                    Transaction transaction) {
        // Начисление средств
        account.setBalance(account.getBalance().add(message.getAmount()));
        accountService.save(account);

        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionService.save(transaction);

        // c. Создание графика платежей для кредитного счета
        if (Boolean.TRUE.equals(account.getIsRecalc())) {
            createPaymentSchedule(account, message.getAmount(), message.getTimestamp());
        }

        // e. Проверка и списание ежемесячного платежа
        if (Boolean.TRUE.equals(account.getIsRecalc())) {
            processMonthlyPayment(account, message.getTimestamp());
        }

        return createSuccessResult(message, account, "Credit transaction processed");
    }

    private TransactionProcessingResultDto processDebitTransaction(TransactionMessageDto message,
                                                                   Account account,
                                                                   Transaction transaction) {
        // Проверка достаточности средств
        if (account.getBalance().compareTo(message.getAmount()) >= 0) {
            account.setBalance(account.getBalance().subtract(message.getAmount()));
            accountService.save(account);

            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionService.save(transaction);

            return createSuccessResult(message, account, "Debit transaction processed");
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionService.save(transaction);

            return createFailedResult(message, "Insufficient funds");
        }
    }

    private void createPaymentSchedule(Account account, BigDecimal amount, LocalDateTime startDate) {
        // Расчет ежемесячного платежа (упрощенная формула)
        BigDecimal monthlyAmount = calculateMonthlyPayment(account, amount);

        // Создание платежей на 12 месяцев (пример)
        for (int i = 1; i <= 12; i++) {
            Payment payment = new Payment();
            payment.setAccount(account);
            payment.setPaymentDate(startDate.plusMonths(i));
            payment.setAmount(monthlyAmount);
            payment.setMonthlyPayment(monthlyAmount);
            payment.setIsCredit(true);
            payment.setType(PaymentType.LOAN_PAYMENT);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setExpired(false);

            paymentService.save(payment);
        }
    }

    private void processMonthlyPayment(Account account, LocalDateTime transactionDate) {
        List<Payment> duePayments = paymentService.findDuePayments(account.getId(), transactionDate);

        for (Payment payment : duePayments) {
            if (account.getBalance().compareTo(payment.getMonthlyPayment()) >= 0) {
                // Списание платежа
                account.setBalance(account.getBalance().subtract(payment.getMonthlyPayment()));
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPayedAt(transactionDate);
            } else {
                // e. Если средств недостаточно - помечаем как expired
                payment.setExpired(true);
                payment.setStatus(PaymentStatus.EXPIRED);
            }
            paymentService.save(payment);
        }
        accountService.save(account);
    }

    private BigDecimal calculateMonthlyPayment(Account account, BigDecimal amount) {
        // Упрощенный расчет аннуитетного платежа
        BigDecimal monthlyRate = account.getInterestRate().divide(BigDecimal.valueOf(1200), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal coefficient = monthlyRate.add(BigDecimal.ONE).pow(12)
                .multiply(monthlyRate)
                .divide(monthlyRate.add(BigDecimal.ONE).pow(12).subtract(BigDecimal.ONE), 6, BigDecimal.ROUND_HALF_UP);

        return amount.multiply(coefficient).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private TransactionProcessingResultDto blockAccountAndCard(TransactionMessageDto message) {
        try {
            Account account = accountService.findById(message.getAccountId());
            Card card = cardService.findByCardId(message.getCardId());

            account.setStatus(AccountStatus.BLOCKED);
            accountService.save(account);

            card.setStatus(org.example.accountModels.enums.CardStatus.BLOCKED);
            cardService.save(card);

            log.warn("Account and card blocked due to suspicious activity: {}", message.getCardId());

            return createBlockedResult(message, "Account blocked due to suspicious activity");
        } catch (Exception e) {
            log.error("Error blocking account: {}", message.getMessageId(), e);
            return createFailedResult(message, "Error blocking account");
        }
    }

    // Вспомогательные методы для создания результатов
    private TransactionProcessingResultDto createSuccessResult(TransactionMessageDto message,
                                                               Account account, String msg) {
        TransactionProcessingResultDto result = new TransactionProcessingResultDto();
        result.setMessageId(message.getMessageId());
        result.setStatus(TransactionStatus.COMPLETED);
        result.setMessage(msg);
        result.setCurrentBalance(account.getBalance());
        result.setAccountBlocked(false);
        return result;
    }

    private TransactionProcessingResultDto createFailedResult(TransactionMessageDto message, String msg) {
        TransactionProcessingResultDto result = new TransactionProcessingResultDto();
        result.setMessageId(message.getMessageId());
        result.setStatus(TransactionStatus.FAILED);
        result.setMessage(msg);
        result.setAccountBlocked(false);
        return result;
    }

    private TransactionProcessingResultDto createBlockedResult(TransactionMessageDto message, String msg) {
        TransactionProcessingResultDto result = new TransactionProcessingResultDto();
        result.setMessageId(message.getMessageId());
        result.setStatus(TransactionStatus.FAILED);
        result.setMessage(msg);
        result.setAccountBlocked(true);
        return result;
    }

    private Transaction createTransaction(TransactionMessageDto message, Account account, Card card) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCard(card);
        transaction.setAmount(message.getAmount());
        transaction.setType(convertTransactionType(message.getType()));
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTimestamp(LocalDateTime.now());
        return transactionService.save(transaction);
    }

    private org.example.accountModels.enums.TransactionType convertTransactionType(
            TransactionType type) {
        return type == TransactionType.CREDIT ?
                org.example.accountModels.enums.TransactionType.CREDIT :
                org.example.accountModels.enums.TransactionType.DEBIT;
    }
}