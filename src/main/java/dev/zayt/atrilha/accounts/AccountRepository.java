package dev.zayt.atrilha.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Acesso a {@link Account} via Spring Data JPA (US-001).
 *
 * <p>Os finders aplicam ambas as regras necessárias para o cadastro:
 * <ul>
 *   <li>{@code IgnoreCase} — e-mail é normalizado em lower-case no service,
 *       mas o derivado garante segurança caso o DBA semeie dados sem
 *       normalização.</li>
 *   <li>{@code DeletedAtIsNull} — soft-delete-aware, para que contas
 *       desativadas não bloqueiem novo cadastro com o mesmo e-mail.</li>
 * </ul>
 * </p>
 */
interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);
}
