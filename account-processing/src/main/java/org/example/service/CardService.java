package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountModels.entity.Account;
import org.example.accountModels.entity.Card;
import org.example.accountModels.enums.CardStatus;
import org.example.dto.CardCreateDto;
import org.example.repository.AccountRepository;
import org.example.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public Card createCard(CardCreateDto cardCreateDto) {
        log.info("Creating card for account ID: {}", cardCreateDto.getAccountId());

        // Проверяем существование счета
        Account account = accountRepository.findById(cardCreateDto.getAccountId())
                .orElseThrow(() -> {
                    log.error("Account not found with ID: {}", cardCreateDto.getAccountId());
                    return new RuntimeException("Account not found with ID: " + cardCreateDto.getAccountId());
                });

        // Проверяем, не существует ли уже карта с таким cardId
        if (cardRepository.existsByCardId(cardCreateDto.getCardId())) {
            log.error("Card with cardId {} already exists", cardCreateDto.getCardId());
            throw new RuntimeException("Card with cardId " + cardCreateDto.getCardId() + " already exists");
        }

        // Создаем и сохраняем карту
        Card card = new Card();
        card.setAccount(account);
        card.setCardId(cardCreateDto.getCardId());
        card.setPaymentSystem(cardCreateDto.getPaymentSystem());
        card.setStatus(CardStatus.ACTIVE); // Статус по умолчанию

        Card savedCard = cardRepository.save(card);

        log.info("Card created successfully with ID: {} for account ID: {}",
                savedCard.getId(), cardCreateDto.getAccountId());

        return savedCard;
    }

    @Transactional(readOnly = true)
    public Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with ID: " + cardId));
    }

    @Transactional(readOnly = true)
    public boolean cardExists(String cardId) {
        return cardRepository.existsByCardId(cardId);
    }
}