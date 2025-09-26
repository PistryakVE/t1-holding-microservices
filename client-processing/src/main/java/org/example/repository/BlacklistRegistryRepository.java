package org.example.repository;

import org.example.clientModels.entity.BlackListRegistry;
import org.example.clientModels.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistRegistryRepository extends JpaRepository<BlackListRegistry, Long> {

    @Query("SELECT COUNT(b) > 0 FROM BlacklistRegistry b WHERE " +
            "b.documentType = :documentType AND b.documentId = :documentId AND " +
            "(b.blacklistExpirationDate IS NULL OR b.blacklistExpirationDate > CURRENT_TIMESTAMP)")
    boolean existsActiveBlacklistEntry(@Param("documentType") DocumentType documentType,
                                       @Param("documentId") String documentId);
}