package org.example.clientModels.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.clientModels.enums.ProductKey;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "product_key", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductKey key;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "product_id", unique = true, nullable = false, updatable = false)
    private String productId;

}
