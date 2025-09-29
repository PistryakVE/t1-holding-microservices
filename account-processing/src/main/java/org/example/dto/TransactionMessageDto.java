package org.example.dto;
import org.example.accountModels.enums.TransactionType;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TransactionMessageDto {
    private UUID messageId; // Ключ сообщения
    private String cardId;
    private String accountId;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime timestamp;

}