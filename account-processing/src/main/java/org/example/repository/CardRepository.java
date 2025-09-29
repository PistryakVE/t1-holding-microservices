package org.example.repository;

import org.example.accountModels.entity.Card;
import org.example.accountModels.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    // Проверяем существование карты по cardId
    boolean existsByCardId(String cardId);

    // Находим карту по cardId
    Optional<Card> findByCardId(String cardId);

    // Находим все карты по accountId
    List<Card> findByAccountId(Long accountId);
    List<Card> findByStatus(CardStatus status);
}