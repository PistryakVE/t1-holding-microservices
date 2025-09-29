package org.example.repository;

import org.example.accountModels.entity.Account;
import org.example.accountModels.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByClientIdAndProductId(String clientId, String productId);
    List<Account> findByClientId(String clientId);
    List<Account> findByStatus(AccountStatus status);
    Optional<Account> findByClientIdAndProductId(String clientId, String productId);
}