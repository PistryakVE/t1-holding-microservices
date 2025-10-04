package org.example.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountModels.entity.Account;
import org.example.accountModels.enums.AccountStatus;
import org.example.aspects.starter.annotation.LogDatasourceError;
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
    @LogDatasourceError(level = LogDatasourceError.LogLevel.ERROR)
    public void consumeCardCreationMessage(@Payload CardCreateDto cardMessage,
                                           Acknowledgment acknowledgment) {
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

        // Создаем карту через сервис - исключение пробрасывается к аспекту
        cardService.createCard(cardMessage);

        log.info("Card created successfully from Kafka message for account ID: {}",
                cardMessage.getAccountId());

        // Подтверждаем обработку сообщения
        acknowledgment.acknowledge();
        log.debug("Message acknowledged successfully");
    }

    /**
     * Проверяет, что аккаунт активен и может иметь карты
     */
    /**
     * Проверяет, что аккаунт активен и может иметь карты
     */
    private boolean isAccountActive(Long accountId) {
        try {
            Optional<Account> accountOpt = accountRepository.findById(accountId);

            if (accountOpt.isEmpty()) {
                log.error("Account not found with ID: {}", accountId);
                // БРОСАЕМ ИСКЛЮЧЕНИЕ ВМЕСТО return false
                throw new RuntimeException("Account not found with ID: " + accountId);
            }

            Account account = accountOpt.get();
            boolean isActive = account.getStatus() == AccountStatus.ACTIVE;

            if (!isActive) {
                log.warn("Account ID: {} has status: {}. Card creation not allowed.",
                        accountId, account.getStatus());
                throw new RuntimeException("Account is not active. ID: " + accountId);
            }

            return true;

        } catch (Exception e) {
            log.error("Error checking account status for account ID: {}", accountId, e);
            // ПРОБРАСЫВАЕМ ИСКЛЮЧЕНИЕ ДАЛЬШЕ
            throw new RuntimeException("Error checking account status", e);
        }
    }
}