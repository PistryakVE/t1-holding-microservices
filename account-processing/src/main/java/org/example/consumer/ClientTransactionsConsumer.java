package org.example.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.TransactionMessageDto;
import org.example.dto.TransactionProcessingResultDto;
import org.example.service.TransactionProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientTransactionsConsumer {

    private final TransactionProcessingService processingService;

    // ИСПРАВЛЕННЫЙ МЕТОД - используем String для ключа
    @KafkaListener(topics = "client_transactions")
    public void consume(@Payload TransactionMessageDto message,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key) { // Изменено на String
        log.info("Received transaction with key: {}, messageId: {}", key, message.getMessageId());

        TransactionProcessingResultDto result = processingService.processTransaction(message);

        log.info("Transaction processed: {}, Status: {}",
                result.getMessageId(), result.getStatus());
    }
}