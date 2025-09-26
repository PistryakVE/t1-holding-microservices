package org.example.dto;

import lombok.Data;

@Data
public class ClientProductMessage {
    private String clientProductId;
    private String clientId;
    private String productId;
    private String productKey;
    private String openDate;
    private String closeDate;
    private String status;
}
