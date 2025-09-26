package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientModels.entity.Client;
import org.example.repository.ClientRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public Client getClientById(String id) {
        log.info("Searching for client with ID: {}", id);

        Client client = clientRepository.findByClientId(id)
                .orElseThrow(() -> {
                    log.warn("Client not found in database with ID: {}", id);
                    return new RuntimeException("Client not found with id: " + id);
                });

        log.info("Client found in database: {} {}", client.getLastName(), client.getFirstName());
        return client;
    }
}