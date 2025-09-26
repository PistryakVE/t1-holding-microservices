package org.example.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ClientProductMessage;
import org.example.service.ProductRegistryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRegistryConsumer {

    private final ProductRegistryService productRegistryService;

    @KafkaListener(
            topics = "client_credit_products"
    )
    public void consumeClientProductCreationMessage(@Payload ClientProductMessage clientProductMessage,
                                           Acknowledgment acknowledgment) {
        try {
            log.info("Received card creation message: {}", clientProductMessage);

            // Проверяем валидность данных
            if (clientProductMessage.getClientId() == null ||
                    clientProductMessage.getProductId() == null ||
                    clientProductMessage.getProductKey() == null ||
                    clientProductMessage.getOpenDate() == null ||
                    clientProductMessage.getStatus() == null){
                log.error("Invalid card creation message: missing required fields");
                acknowledgment.acknowledge();
                return;
            }

            // Создаем карту через сервис
            productRegistryService.createClientProduct(clientProductMessage);

            // Подтверждаем обработку сообщения
            acknowledgment.acknowledge();
            log.debug("Message acknowledged successfully");

        } catch (Exception e) {
            log.error("Error processing productRegistry creation message for account ID: {}",
                    clientProductMessage.getClientId(), e);
            // Не бросаем исключение, чтобы не ломать контейнер
            // Можно добавить логику повторной обработки или отправки в DLQ
        }
    }
}