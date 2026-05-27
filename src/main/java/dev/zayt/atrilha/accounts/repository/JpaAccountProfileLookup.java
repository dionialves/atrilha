package dev.zayt.atrilha.accounts.repository;

import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import dev.zayt.atrilha.accounts.domain.GuardianProfile;
import dev.zayt.atrilha.accounts.repository.AdolescentProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação JPA de {@link AccountProfileLookup} — busca o apelido do
 * adolescente via {@link AdolescentProfileRepository}. Package-private
 * porque ninguém fora de {@code accounts} precisa referenciar a classe.
 */
@Component
class JpaAccountProfileLookup implements AccountProfileLookup {

    private final AdolescentProfileRepository adolescentProfileRepository;
    private final GuardianProfileRepository guardianProfileRepository;

    JpaAccountProfileLookup(AdolescentProfileRepository adolescentProfileRepository,
                            GuardianProfileRepository guardianProfileRepository) {
        this.adolescentProfileRepository = adolescentProfileRepository;
        this.guardianProfileRepository = guardianProfileRepository;
    }

    @Override
    public Optional<String> findNickname(UUID accountId) {
        return adolescentProfileRepository.findById(accountId)
                .map(AdolescentProfile::getNickname);
    }

    @Override
    public Optional<String> findFullName(UUID accountId) {
        return guardianProfileRepository.findByAccountId(accountId)
                .map(GuardianProfile::getFullName);
    }

    @Override
    public Optional<String> findDisplayName(UUID accountId, String accountType) {
        if ("GUARDIAN".equals(accountType)) {
            return findFullName(accountId);
        }
        return findNickname(accountId);
    }
}
