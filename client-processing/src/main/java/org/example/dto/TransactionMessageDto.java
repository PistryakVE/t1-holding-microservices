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
public class TransactionMessageDto {
    private UUID messageId;
    private String cardId;
    private String accountId;
    private BigDecimal amount;
    private TransactionType type;

    private LocalDateTime timestamp;

    public enum TransactionType {
        CREDIT,  // Начисление
        DEBIT    // Списание
    }
}