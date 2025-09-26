package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ClientInfoDto;
import org.example.clientModels.entity.Client;
import org.example.service.ClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/getclients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping("/{id}")
    public ResponseEntity<ClientInfoDto> getClientById(@PathVariable String id) {
        try {
            log.info("=== GET REQUEST RECEIVED ===");
            log.info("Request for client ID: {}", id);

            Client client = clientService.getClientById(id);
            ClientInfoDto clientInfo = new ClientInfoDto(client);

            log.info("Client found: {} {} {}",
                    clientInfo.getLastName(),
                    clientInfo.getFirstName(),
                    clientInfo.getMiddleName());
            log.info("Document ID: {}", clientInfo.getDocumentNumber());
            log.info("=== RESPONSE SENT ===");

            return ResponseEntity.ok(clientInfo);
        } catch (RuntimeException e) {
            log.error("Client not found with ID: {}", id);
            log.info("=== RESPONSE: 404 NOT FOUND ===");
            return ResponseEntity.notFound().build();
        }
    }
}