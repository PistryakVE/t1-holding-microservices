package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.PaymentMessageDto;
import org.example.accountModels.entity.Account;
import org.example.accountModels.entity.Payment;
import org.example.accountModels.enums.PaymentStatus;
import org.example.accountModels.enums.PaymentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final AccountService accountService;
    private final PaymentService paymentService;

    @Transactional
    public void processPayment(PaymentMessageDto message) {
        Account account = accountService.findById(message.getAccountId());

        if (Boolean.TRUE.equals(account.getIsRecalc())) {
            processCreditAccountPayment(message, account);
        } else {
            processRegularPayment(message, account);
        }
    }

    private void processCreditAccountPayment(PaymentMessageDto message, Account account) {
        BigDecimal totalDebt = calculateTotalDebt(account.getId());

        // ДОБАВЬТЕ ДИАГНОСТИЧЕСКОЕ ЛОГИРОВАНИЕ
        log.info("Account {}: Payment amount = {}, Total debt = {}",
                account.getId(), message.getAmount(), totalDebt);

        if (message.getAmount().compareTo(totalDebt) == 0) {
            processFullRepayment(message, account, totalDebt);
        } else {
            processPartialPayment(message, account);
        }
    }

    private void processFullRepayment(PaymentMessageDto message, Account account, BigDecimal totalDebt) {
        log.info("Processing full repayment for account: {}, amount: {}", account.getId(), totalDebt);

        // 1. ✅ УВЕЛИЧИВАЕМ баланс на сумму платежа (клиент вносит деньги)
        BigDecimal newBalance = account.getBalance().add(message.getAmount());
        account.setBalance(newBalance);
        accountService.save(account);

        // 2. Создание ОДНОГО платежа Payment
        createSinglePaymentRecord(message, account, PaymentType.EARLY_REPAYMENT, totalDebt);

        // 3. Обновление существующих записей Payment
        updateExistingPayments(account.getId());

        log.info("Full repayment completed for account: {}", account.getId());
    }

    private void processPartialPayment(PaymentMessageDto message, Account account) {
        log.info("Processing partial payment for account: {}, amount: {}", account.getId(), message.getAmount());

        account.setBalance(account.getBalance().add(message.getAmount()));
        accountService.save(account);

        createSinglePaymentRecord(message, account, message.getPaymentType(), message.getAmount());
        processAutomaticPaymentDeduction(account);
    }

    private void processRegularPayment(PaymentMessageDto message, Account account) {
        log.info("Processing regular payment for account: {}, amount: {}", account.getId(), message.getAmount());

        account.setBalance(account.getBalance().add(message.getAmount()));
        accountService.save(account);

        createSinglePaymentRecord(message, account, PaymentType.REGULAR_PAYMENT, message.getAmount());
    }

    private BigDecimal calculateTotalDebt(Long accountId) {
        List<Payment> pendingPayments = paymentService.findPendingPaymentsByAccount(accountId);

        // ДОБАВЬТЕ ЛОГИРОВАНИЕ ДЛЯ ДИАГНОСТИКИ
        log.info("Found {} pending payments for account {}", pendingPayments.size(), accountId);
        pendingPayments.forEach(p ->
                log.info("Payment {}: monthly={}, status={}", p.getId(), p.getMonthlyPayment(), p.getStatus()));

        BigDecimal total = pendingPayments.stream()
                .map(Payment::getMonthlyPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total debt calculated: {}", total);
        return total;
    }

    private void updateExistingPayments(Long accountId) {
        List<Payment> pendingPayments = paymentService.findPendingPaymentsByAccount(accountId);

        for (Payment payment : pendingPayments) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPayedAt(LocalDateTime.now());
            paymentService.save(payment);
        }

        log.info("Updated {} pending payments for account: {}", pendingPayments.size(), accountId);
    }

    private void createSinglePaymentRecord(PaymentMessageDto message, Account account,
                                           PaymentType paymentType, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setAccount(account);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setAmount(amount);

        // ✅ ИСПРАВЛЕНО: Для полного погашения monthlyPayment должно быть 0 или null
        // поскольку это разовый платеж, а не регулярный
        if (paymentType == PaymentType.EARLY_REPAYMENT) {
            payment.setMonthlyPayment(BigDecimal.ZERO); // или null
        } else {
            payment.setMonthlyPayment(amount); // для регулярных платежей
        }

        payment.setIsCredit(true);
        payment.setType(convertPaymentType(paymentType));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPayedAt(LocalDateTime.now());
        payment.setExpired(false);

        paymentService.save(payment);
    }

    private void processAutomaticPaymentDeduction(Account account) {
        List<Payment> duePayments = paymentService.findDuePayments(account.getId(), LocalDateTime.now());

        for (Payment payment : duePayments) {
            if (account.getBalance().compareTo(payment.getMonthlyPayment()) >= 0) {
                // Списание платежа
                account.setBalance(account.getBalance().subtract(payment.getMonthlyPayment()));
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPayedAt(LocalDateTime.now());
                paymentService.save(payment);

                log.info("Automatically deducted payment: {} for account: {}",
                        payment.getMonthlyPayment(), account.getId());
            }
        }
        accountService.save(account);
    }

    private org.example.accountModels.enums.PaymentType convertPaymentType(PaymentType type) {
        switch (type) {
            case LOAN_PAYMENT:
                return org.example.accountModels.enums.PaymentType.LOAN_PAYMENT;
            case EARLY_REPAYMENT:
                return org.example.accountModels.enums.PaymentType.EARLY_REPAYMENT; // Сохраняем семантику
            case REGULAR_PAYMENT:
                return org.example.accountModels.enums.PaymentType.DEPOSIT;
            default:
                return org.example.accountModels.enums.PaymentType.DEPOSIT;
        }
    }
}