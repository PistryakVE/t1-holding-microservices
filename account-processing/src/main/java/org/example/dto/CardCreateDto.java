package org.example.dto;

import lombok.Data;

@Data
public class CardCreateDto {
    private Long accountId;
    private String cardId;
    private String paymentSystem;
}