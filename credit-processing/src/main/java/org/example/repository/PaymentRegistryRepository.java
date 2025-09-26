package org.example.repository;

import org.example.creditModels.PaymentRegistry;
import org.example.creditModels.ProductRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRegistryRepository extends JpaRepository<PaymentRegistry, Long> {

    // Исправить все запросы - использовать строковый clientId
    @Query("SELECT pr FROM PaymentRegistry pr WHERE pr.productRegistry.clientId = :clientId AND pr.expired = true")
    List<PaymentRegistry> findOverduePaymentsByClientId(@Param("clientId") String clientId);

    @Query("SELECT pr FROM PaymentRegistry pr WHERE pr.productRegistry.clientId = :clientId")
    List<PaymentRegistry> findAllPaymentsByClientId(@Param("clientId") String clientId);
}