package org.example.dto;

import lombok.Data;
import org.example.accountModels.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PaymentMessageDto {
    private UUID messageId;
    private String accountId;
    private BigDecimal amount;
    private PaymentType paymentType;
    private LocalDateTime timestamp;
    private String description;


}