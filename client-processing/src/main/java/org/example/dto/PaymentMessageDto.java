package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMessageDto {
    private UUID messageId;
    private String accountId;
    private BigDecimal amount;
    private PaymentType paymentType;

    private LocalDateTime timestamp;

    private String description;

    public enum PaymentType {
        LOAN_PAYMENT,    // Платеж по кредиту
        EARLY_REPAYMENT, // Досрочное погашение
        REGULAR_PAYMENT  // Регулярный платеж
    }
}