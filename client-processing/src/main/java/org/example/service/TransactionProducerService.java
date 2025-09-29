package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.TransactionMessageDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;


    public void sendTransaction(TransactionMessageDto transactionMessage) {
        String key = transactionMessage.getMessageId().toString();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send("client_transactions", key, transactionMessage);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transaction sent successfully: {}, offset: {}",
                        transactionMessage.getMessageId(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send transaction: {}, error: {}",
                        transactionMessage.getMessageId(), ex.getMessage());
            }
        });
    }

}