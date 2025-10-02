package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.aspect.annotation.HttpIncomeRequestLog;
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

    @HttpIncomeRequestLog
    @GetMapping("/{id}")
    public ResponseEntity<ClientInfoDto> getClientById(@PathVariable String id) {
        try {
            Client client = clientService.getClientById(id);
            ClientInfoDto clientInfo = new ClientInfoDto(client);

            return ResponseEntity.ok(clientInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}