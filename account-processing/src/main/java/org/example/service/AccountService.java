package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.accountModels.entity.Account;
import org.example.accountModels.enums.AccountStatus;
//import org.example.aspect.annotation.Cached;
import org.example.aspects.starter.annotation.Cached;
import org.example.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Cached(cacheName = "accounts-by-id", ttl = 900000)
    public Account findById(String accountId) {
        return accountRepository.findById(Long.valueOf(accountId))
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
    }

    public Account save(Account account) {
        return accountRepository.save(account);
    }

    @Cached(cacheName = "accounts-by-client", ttl = 600000)
    public List<Account> findByClientId(String clientId) {
        return accountRepository.findByClientId(clientId);
    }

    @Cached(cacheName = "accounts-by-status", ttl = 300000)
    public List<Account> findByStatus(AccountStatus status) {
        return accountRepository.findByStatus(status);
    }

    public void updateBalance(Long accountId, BigDecimal amount, boolean isCredit) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (isCredit) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            if (account.getBalance().compareTo(amount) >= 0) {
                account.setBalance(account.getBalance().subtract(amount));
            } else {
                throw new RuntimeException("Insufficient funds");
            }
        }
        accountRepository.save(account);
    }

    public void blockAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
    }

    @Cached(cacheName = "account-active-status", ttl = 300000)
    public boolean isAccountActive(Long accountId) {
        return accountRepository.findById(accountId)
                .map(account -> account.getStatus() == AccountStatus.ACTIVE)
                .orElse(false);
    }
}