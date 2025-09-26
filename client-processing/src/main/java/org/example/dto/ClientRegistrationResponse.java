package org.example.dto;

import lombok.Data;

@Data
public class ClientRegistrationResponse {
    private String message;
    private String clientId;
    private Long userId;
    private Long clientProfileId;
}