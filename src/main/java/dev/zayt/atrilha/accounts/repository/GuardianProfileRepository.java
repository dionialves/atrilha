package dev.zayt.atrilha.accounts.repository;

import dev.zayt.atrilha.accounts.domain.GuardianProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a {@link GuardianProfile} via Spring Data JPA (US-003).
 *
 * <p>Sem queries customizadas — leituras de perfil partem do
 * {@code accountId} (PK compartilhada via {@code @MapsId}).</p>
 */
public interface GuardianProfileRepository
    extends JpaRepository<GuardianProfile, UUID>
{
    /** Busca o perfil do responsável pelo identificador da conta (US-003). */
    Optional<GuardianProfile> findByAccountId(UUID accountId);
}
