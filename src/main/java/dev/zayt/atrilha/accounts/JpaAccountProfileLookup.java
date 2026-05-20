package dev.zayt.atrilha.accounts;

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

    JpaAccountProfileLookup(AdolescentProfileRepository adolescentProfileRepository) {
        this.adolescentProfileRepository = adolescentProfileRepository;
    }

    @Override
    public Optional<String> findNickname(UUID accountId) {
        return adolescentProfileRepository.findById(accountId)
                .map(AdolescentProfile::getNickname);
    }
}
