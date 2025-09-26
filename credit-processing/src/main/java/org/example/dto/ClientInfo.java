package org.example.dto;

import lombok.Data;

@Data
public class ClientInfo {
    private Long id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String documentNumber;
}