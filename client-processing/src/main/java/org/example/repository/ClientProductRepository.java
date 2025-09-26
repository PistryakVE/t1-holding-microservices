package org.example.repository;

import org.example.clientModels.entity.ClientProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientProductRepository extends JpaRepository<ClientProduct, Long> {

    @Query("SELECT cp FROM ClientProduct cp WHERE cp.client.id = :clientId")
    List<ClientProduct> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT cp FROM ClientProduct cp WHERE cp.product.id = :productId")
    List<ClientProduct> findByProductId(@Param("productId") Long productId);

    List<ClientProduct> findByStatus(String status);
}