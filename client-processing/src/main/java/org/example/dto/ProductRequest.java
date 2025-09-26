package org.example.dto;

import lombok.Data;
import org.example.clientModels.enums.ProductKey;

import java.time.LocalDateTime;

@Data
public class ProductRequest {
    private String name;
    private ProductKey key;
    private LocalDateTime createDate;
    private String productId;
}