package org.example.accountModels.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.accountModels.enums.AccountStatus;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Data
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "interest_rate")
    private BigDecimal interestRate;

    @Column(name = "is_recalc")
    private Boolean isRecalc;

    @Column(name = "card_exist")
    private Boolean cardExist;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
}

