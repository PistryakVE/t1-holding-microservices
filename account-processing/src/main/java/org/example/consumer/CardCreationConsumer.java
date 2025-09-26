package org.example.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountModels.entity.Account;
import org.example.accountModels.enums.AccountStatus;
import org.example.dto.CardCreateDto;
import org.example.service.CardService;
import org.example.repository.AccountRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardCreationConsumer {

    private final CardService cardService;
    private final AccountRepository accountRepository;

    @KafkaListener(topics = "client_cards")
    public void consumeCardCreationMessage(@Payload CardCreateDto cardMessage,
                                           Acknowledgment acknowledgment) {
        try {
            log.info("Received card creation message: {}", cardMessage);

            if (cardMessage.getAccountId() == null ||
                    cardMessage.getCardId() == null ||
                    cardMessage.getPaymentSystem() == null) {
                log.error("Invalid card creation message: missing required fields");
                acknowledgment.acknowledge();
                return;
            }

            if (!isAccountActive(cardMessage.getAccountId())) {
                log.error("Cannot create card for account ID: {}. Account is not active or not found",
                        cardMessage.getAccountId());
                acknowledgment.acknowledge();
                return;
            }

            // Создаем карту через сервис
            cardService.createCard(cardMessage);

            log.info("Card created successfully from Kafka message for account ID: {}",
                    cardMessage.getAccountId());

            // Подтверждаем обработку сообщения
            acknowledgment.acknowledge();
            log.debug("Message acknowledged successfully");

        } catch (Exception e) {
            log.error("Error processing card creation message for account ID: {}",
                    cardMessage.getAccountId(), e);
            // Можно добавить логику повторной обработки или отправки в DLQ
        }
    }

    /**
     * Проверяет, что аккаунт активен и может иметь карты
     */
    private boolean isAccountActive(Long accountId) {
        try {
            Optional<Account> accountOpt = accountRepository.findById(accountId);

            if (accountOpt.isEmpty()) {
                log.error("Account not found with ID: {}", accountId);
                return false;
            }

            Account account = accountOpt.get();
            boolean isActive = account.getStatus() == AccountStatus.ACTIVE;

            if (!isActive) {
                log.warn("Account ID: {} has status: {}. Card creation not allowed.",
                        accountId, account.getStatus());
            }

            return isActive;

        } catch (Exception e) {
            log.error("Error checking account status for account ID: {}", accountId, e);
            return false; // В случае ошибки считаем аккаунт неактивным
        }
    }
}