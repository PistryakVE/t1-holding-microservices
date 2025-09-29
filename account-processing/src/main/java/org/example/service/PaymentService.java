package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.accountModels.entity.Payment;
import org.example.accountModels.enums.PaymentStatus;
import org.example.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    public List<Payment> findByAccountId(Long accountId) {
        return paymentRepository.findByAccountId(accountId);
    }

    public List<Payment> findDuePayments(Long accountId, LocalDateTime currentDate) {
        return paymentRepository.findByAccountIdAndPaymentDateBeforeAndStatus(
                accountId, currentDate, PaymentStatus.PENDING);
    }

    public List<Payment> findOverduePayments(Long accountId) {
        return paymentRepository.findByAccountIdAndExpiredTrue(accountId);
    }

    public List<Payment> findCompletedPaymentsByAccount(Long accountId) {
        return paymentRepository.findByAccountIdAndStatus(accountId, PaymentStatus.COMPLETED);
    }

    public void markPaymentAsPaid(Long paymentId, LocalDateTime payedAt) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPayedAt(payedAt);
        payment.setExpired(false);
        paymentRepository.save(payment);
    }

    public void markPaymentAsExpired(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setExpired(true);
        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);
    }

    public List<Payment> findPendingPaymentsByAccount(Long accountId) {
        return paymentRepository.findByAccountIdAndStatus(accountId, PaymentStatus.PENDING);
    }

    public boolean hasOverduePayments(Long accountId) {
        return paymentRepository.countByAccountIdAndExpiredTrue(accountId) > 0;
    }
}