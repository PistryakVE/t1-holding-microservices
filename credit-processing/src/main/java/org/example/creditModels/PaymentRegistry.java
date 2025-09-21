package org.example.creditModels;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment_registry")
@Data
public class PaymentRegistry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_registry_id", nullable = false)
    private ProductRegistry productRegistry;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "interest_rate_amount", nullable = false)
    private BigDecimal interestRateAmount;

    @Column(name = "debt_amount", nullable = false)
    private BigDecimal debtAmount;

    @Column(nullable = false)
    private Boolean expired;

    @Column(name = "payment_expiration_date", nullable = false)
    private LocalDate paymentExpirationDate;
}