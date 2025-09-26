package org.example.dto;

import lombok.Data;
import org.example.clientModels.entity.Client;

@Data
public class ClientInfoDto {
    private String firstName;
    private String lastName;
    private String middleName;
    private String documentNumber;

    // Конструктор для создания из Entity Client
    public ClientInfoDto(Client client) {
        this.firstName = client.getFirstName();
        this.lastName = client.getLastName();
        this.middleName = client.getMiddleName();
        this.documentNumber = client.getDocumentId(); // Только documentId!
    }
}