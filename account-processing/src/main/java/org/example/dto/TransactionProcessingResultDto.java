package org.example.dto;

import lombok.Data;
import org.example.accountModels.enums.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransactionProcessingResultDto {
    private UUID messageId;
    private TransactionStatus status;
    private String message;
    private BigDecimal currentBalance;
    private Boolean accountBlocked;
}