package org.example.clientModels.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.clientModels.enums.DocumentType;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklist_registry")
@Data
public class BlackListRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "blacklisted_at", nullable = false)
    private LocalDateTime blacklistedAt;

    @Column(name = "reason")
    private String reason;

    @Column(name = "blacklist_expiration_date")
    private LocalDateTime blacklistExpirationDate;
}