package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.TransactionMessageDto;
import org.example.service.TransactionProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionProducerService transactionProducerService;

    @PostMapping("/send")
    public ResponseEntity<String> sendTransaction(@RequestBody TransactionMessageDto transactionMessage) {
        try {
            transactionProducerService.sendTransaction(transactionMessage);
            return ResponseEntity.ok("Transaction sent successfully: " + transactionMessage.getMessageId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send transaction: " + e.getMessage());
        }
    }

}