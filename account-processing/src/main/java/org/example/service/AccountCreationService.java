package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountModels.entity.Account;
import org.example.accountModels.enums.AccountStatus;
import org.example.dto.ClientProductMessage;
import org.example.repository.AccountRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCreationService {

    private final AccountRepository accountRepository;

    @KafkaListener(topics = "client_products")
    @Transactional
    public void createAccountFromKafkaMessage(@Payload ClientProductMessage message,
                                              Acknowledgment acknowledgment) {
        try {
            log.info("Received message from Kafka: {}", message);

            // Проверяем, что это создание нового продукта (не обновление)
            if (!"ACTIVE".equals(message.getStatus()) || !"PENDING".equals(message.getClientProductId())) {
                log.info("Skipping message - not a new active product creation");
                acknowledgment.acknowledge();
                return;
            }

            // Проверяем, не существует ли уже аккаунт для этой комбинации
            boolean accountExists = accountRepository.existsByClientIdAndProductId(
                    message.getClientId(),
                    message.getProductId()
            );

            if (accountExists) {
                log.warn("Account already exists for clientId: {}, productId: {}",
                        message.getClientId(), message.getProductId());
                acknowledgment.acknowledge();
                return;
            }

            // Создаем новый аккаунт
            Account account = createAccountFromMessage(message);
            Account savedAccount = accountRepository.save(account);

            log.info("Successfully created account with id: {} for clientId: {}, productId: {}",
                    savedAccount.getId(), message.getClientId(), message.getProductId());

            // Подтверждаем обработку сообщения
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
            // В реальном приложении здесь должна быть логика повторной обработки
            throw new RuntimeException("Failed to process Kafka message", e);
        }
    }

    private Account createAccountFromMessage(ClientProductMessage message) {
        Account account = new Account();

        account.setClientId(message.getClientId());
        account.setProductId(message.getProductId());
        account.setBalance(BigDecimal.ZERO); // Начальный баланс 0
        account.setStatus(AccountStatus.ACTIVE);

        // Устанавливаем значения по умолчанию в зависимости от типа продукта
        setDefaultValuesByProductType(account, message.getProductKey());

        return account;
    }

    private void setDefaultValuesByProductType(Account account, String productKey) {
        switch (productKey) {
            case "DC": // Дебетовая карта
                account.setInterestRate(BigDecimal.ZERO);
                account.setIsRecalc(false);
                account.setCardExist(true);
                break;
            case "CC": // Кредитная карта
                account.setInterestRate(new BigDecimal("12.5"));
                account.setIsRecalc(true);
                account.setCardExist(true);
                break;
            case "NS": // Накопительный счет
                account.setInterestRate(new BigDecimal("5.2"));
                account.setIsRecalc(true);
                account.setCardExist(false);
                break;
            case "PENS": // Пенсионный счет
                account.setInterestRate(new BigDecimal("6.8"));
                account.setIsRecalc(true);
                account.setCardExist(false);
                break;
            default:
                account.setInterestRate(BigDecimal.ZERO);
                account.setIsRecalc(false);
                account.setCardExist(false);
        }
    }

}