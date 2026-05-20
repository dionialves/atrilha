package dev.zayt.atrilha.accounts;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação JPA de {@link AccountReader}. Mantém o
 * {@link AccountRepository} package-private — apenas os métodos necessários
 * cruzam a fronteira.
 */
@Component
class JpaAccountReader implements AccountReader {

    private final AccountRepository accountRepository;

    JpaAccountReader(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return accountRepository.findById(accountId);
    }

    @Override
    @Transactional
    public void markEmailVerifiedAt(UUID accountId, OffsetDateTime when) {
        accountRepository.findById(accountId).ifPresent(account -> {
            if (account.getEmailVerifiedAt() == null) {
                account.setEmailVerifiedAt(when);
                accountRepository.saveAndFlush(account);
            }
        });
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        if (email == null) {
            return false;
        }
        return accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email);
    }
}
