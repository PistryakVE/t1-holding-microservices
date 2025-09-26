package org.example.dto;

import lombok.Data;
import org.example.clientModels.enums.DocumentType;

import java.time.LocalDate;

@Data
public class ClientRegistrationRequest {
    private String login;
    private String password;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dateOfBirth;
    private DocumentType documentType;
    private String documentId;
    private String documentPrefix;
    private String documentSuffix;
}