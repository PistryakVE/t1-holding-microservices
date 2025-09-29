package org.example.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.PaymentMessageDto;
import org.example.service.PaymentProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientPaymentsConsumer {

    private final PaymentProcessingService paymentProcessingService;

    @KafkaListener(topics = "client_payments")
    public void consume(@Payload PaymentMessageDto message) {
        try {
            log.info("Received payment: {} for account: {}", message.getMessageId(), message.getAccountId());

            paymentProcessingService.processPayment(message);

            log.info("Payment processed successfully: {}", message.getMessageId());
        } catch (Exception e) {
            log.error("Error processing payment: {}", message.getMessageId(), e);
        }
    }
}