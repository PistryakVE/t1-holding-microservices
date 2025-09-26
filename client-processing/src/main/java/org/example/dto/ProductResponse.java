package org.example.dto;

import lombok.Data;
import org.example.clientModels.enums.ProductKey;

import java.time.LocalDateTime;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private ProductKey key;
    private LocalDateTime createDate;
    private String productId;
}