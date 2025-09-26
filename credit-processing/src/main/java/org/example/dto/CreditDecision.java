package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CreditDecision {
    private boolean approved;
    private String reason;
    private BigDecimal totalDebt;
    private boolean hasOverdue;
    private int overdueCount;
    private String clientName;
    private int activeProductsCount;
}