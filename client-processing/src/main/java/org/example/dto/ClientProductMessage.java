package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientProductMessage {
    private String clientProductId;
    private String clientId;
    private String productId;
    private String productKey;
    private String openDate;
    private String closeDate;
    private String status;
}