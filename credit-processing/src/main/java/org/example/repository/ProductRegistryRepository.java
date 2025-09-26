package org.example.repository;

import org.example.creditModels.ProductRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRegistryRepository extends JpaRepository<ProductRegistry, Long> {
    // Исправить - искать по строке
    List<ProductRegistry> findByClientId(String clientId);
}
