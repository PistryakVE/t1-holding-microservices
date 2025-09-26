package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.CardCreateDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class CardCreateService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CardCreateDto createCard(CardCreateDto cardCreateDto) {
        try {
            // Проверяем обязательные поля
            if (cardCreateDto.getAccountId() == null) {
                throw new RuntimeException("Account ID is required");
            }
            if (cardCreateDto.getCardId() == null || cardCreateDto.getCardId().trim().isEmpty()) {
                throw new RuntimeException("Card ID is required");
            }
            if (cardCreateDto.getPaymentSystem() == null || cardCreateDto.getPaymentSystem().trim().isEmpty()) {
                throw new RuntimeException("Payment system is required");
            }

            // Отправляем сообщение в Kafka топик client_cards
            CompletableFuture<SendResult<String, Object>> future = sendToKafka(cardCreateDto);

            // Ждем подтверждения от Kafka (таймаут 10 секунд)
            SendResult<String, Object> result = future.get(10, TimeUnit.SECONDS);
            System.out.println("Message sent to Kafka topic 'client_cards': " + result.getRecordMetadata());

            // Возвращаем тот же DTO (или можно создать response DTO)
            return cardCreateDto;

        } catch (TimeoutException e) {
            System.err.println("Kafka timeout - message not delivered within 10 seconds");
            throw new RuntimeException("Kafka timeout - please try again later");
        } catch (Exception e) {
            System.err.println("Error sending message to Kafka: " + e.getMessage());
            throw new RuntimeException("Failed to create card: " + e.getMessage());
        }
    }

    private CompletableFuture<SendResult<String, Object>> sendToKafka(CardCreateDto cardCreateDto) {
        try {
            System.out.println("Sending card creation message to Kafka topic: client_cards");

            // Отправляем сам DTO объект в топик client_cards
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send("client_cards", cardCreateDto.getCardId(), cardCreateDto);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    System.err.println("Kafka send failed: " + exception.getMessage());
                } else {
                    System.out.println("Kafka send successful: " + result.getRecordMetadata());
                }
            });

            return future;

        } catch (Exception e) {
            System.err.println("Error preparing Kafka message: " + e.getMessage());
            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }
}