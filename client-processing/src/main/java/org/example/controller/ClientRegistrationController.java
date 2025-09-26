package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ClientRegistrationRequest;
import org.example.dto.ClientRegistrationResponse;
import org.example.service.ClientRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientRegistrationController {

    private final ClientRegistrationService clientRegistrationService;

    @PostMapping("/register")
    public ResponseEntity<ClientRegistrationResponse> registerClient(
            @RequestBody ClientRegistrationRequest request) {

        ClientRegistrationResponse response = clientRegistrationService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}