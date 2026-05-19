package dev.zayt.atrilha.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Acesso a {@link AdolescentProfile} via Spring Data JPA (US-001).
 *
 * <p>Sem queries customizadas — leituras de perfil partem do
 * {@code accountId} (PK compartilhada via {@code @MapsId}).</p>
 */
interface AdolescentProfileRepository extends JpaRepository<AdolescentProfile, UUID> {
}
