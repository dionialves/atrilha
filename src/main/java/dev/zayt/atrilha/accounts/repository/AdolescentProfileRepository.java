package dev.zayt.atrilha.accounts.repository;

import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a {@link AdolescentProfile} via Spring Data JPA (US-001).
 *
 * <p>Sem queries customizadas — leituras de perfil partem do
 * {@code accountId} (PK compartilhada via {@code @MapsId}).</p>
 */
public interface AdolescentProfileRepository
    extends JpaRepository<AdolescentProfile, UUID>
{
    /** Busca o perfil do adolescente pelo identificador da conta (US-001 / FIX-016). */
    Optional<AdolescentProfile> findByAccountId(UUID accountId);
}
