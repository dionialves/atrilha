package dev.zayt.atrilha.auth.verification;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso a {@link PasswordResetToken} via Spring Data JPA (US-008-a).
 *
 * <p>Finders cobrem os cenários do service:
 * <ul>
 *   <li>{@link #findByToken(UUID)} — leitura simples (verificações de estado,
 *       testes, diagnóstico).</li>
 *   <li>{@link #findByTokenForUpdate(UUID)} — leitura com {@code PESSIMISTIC_WRITE},
 *       usada pelo {@code verify(UUID)} para serializar verificações
 *       concorrentes do mesmo token.</li>
 *   <li>{@link #findByAccountIdAndUsedAtIsNull(UUID)} — lista de tokens ativos
 *       de um usuário (resend marca todos como usados).</li>
 *   <li>{@link #countByAccountIdAndCreatedAtAfter(UUID, Instant)} — rate-limit
 *       de emissões por hora por usuário.</li>
 *   <li>{@link #findFirstByAccountIdOrderByCreatedAtDesc(UUID)} — cooldown
 *       entre reenvios consecutivos.</li>
 * </ul>
 * </p>
 */
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(UUID token);

    /**
     * Busca o token adquirindo lock pessimista de escrita na linha
     * ({@code SELECT ... FOR UPDATE}). Usado pelo {@code verify(UUID)} para
     * impedir que duas chamadas concorrentes do mesmo token retornem ambos
     * {@code SUCCESS}. O chamador <strong>deve</strong> estar dentro de uma
     * transação ativa ({@code @Transactional} no service).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.token = :token")
    Optional<PasswordResetToken> findByTokenForUpdate(@Param("token") UUID token);

    List<PasswordResetToken> findByAccountIdAndUsedAtIsNull(UUID accountId);

    long countByAccountIdAndCreatedAtAfter(UUID accountId, Instant since);

    Optional<PasswordResetToken> findFirstByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
