package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.PaymentMessageDto;
import org.example.service.PaymentProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentProducerService paymentProducerService;

    @PostMapping("/send")
    public ResponseEntity<String> sendPayment(@RequestBody PaymentMessageDto paymentMessage) {
        try {
            paymentProducerService.sendPayment(paymentMessage);
            return ResponseEntity.ok("Payment sent successfully: " + paymentMessage.getMessageId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send payment: " + e.getMessage());
        }
    }


}