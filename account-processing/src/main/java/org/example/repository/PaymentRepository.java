package org.example.repository;

import org.example.accountModels.entity.Payment;
import org.example.accountModels.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByAccountId(Long accountId);
    List<Payment> findByAccountIdAndPaymentDateBeforeAndStatus(Long accountId, LocalDateTime date, PaymentStatus status);
    List<Payment> findByAccountIdAndExpiredTrue(Long accountId);
    List<Payment> findByAccountIdAndStatus(Long accountId, PaymentStatus status);
    long countByAccountIdAndExpiredTrue(Long accountId);

    @Query("SELECT p FROM Payment p WHERE p.account.id = :accountId AND p.paymentDate <= :currentDate AND p.status = 'PENDING' ORDER BY p.paymentDate ASC")
    List<Payment> findDuePayments(@Param("accountId") Long accountId, @Param("currentDate") LocalDateTime currentDate);
}