package org.example.accountModels.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.accountModels.enums.CardStatus;

@Entity
@Table(name = "cards")
@Data
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "card_id", nullable = false, unique = true)
    private String cardId;

    @Column(name = "payment_system", nullable = false)
    private String paymentSystem;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardStatus status;
}



