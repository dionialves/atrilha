package dev.zayt.atrilha.accounts;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Leitura/atualização cross-module de {@link Account} para operações
 * estritamente necessárias fora do módulo {@code accounts}.
 *
 * <p>A interface mantém {@code Account} acessível mas restringe os métodos
 * aos casos de uso explícitos:
 * <ul>
 *   <li>{@link #findById(UUID)} — verificação de e-mail e banner consultam o
 *       estado da conta para decidir comportamento.</li>
 *   <li>{@link #markEmailVerifiedAt(UUID, OffsetDateTime)} — a US-006 marca
 *       o e-mail como verificado sem precisar abrir a entidade.</li>
 * </ul>
 * </p>
 */
public interface AccountReader {

    Optional<Account> findById(UUID accountId);

    /**
     * Marca {@code email_verified_at} no momento dado, apenas se ainda for
     * {@code NULL}. Não-op (no-op) se a conta já estava verificada
     * (preserva timestamp original — US-006 critério de idempotência).
     */
    void markEmailVerifiedAt(UUID accountId, OffsetDateTime when);

    /**
     * Busca conta nao soft-deletada por e-mail (case-insensitive).
     * Usada por {@code JpaLoginAccountQuery} (FIX-013) para servir o
     * {@code UserDetailsService} do Spring Security.
     */
    java.util.Optional<Account> findByEmailIgnoreCase(String email);
}
