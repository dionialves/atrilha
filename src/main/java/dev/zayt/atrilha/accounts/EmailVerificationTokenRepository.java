package dev.zayt.atrilha.accounts;

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
 * Acesso a {@link EmailVerificationToken} via Spring Data JPA (US-006).
 *
 * <p>Finders cobrem os três cenários do service:
 * <ul>
 *   <li>{@link #findByToken(UUID)} — leitura simples (verificações de estado,
 *       testes, diagnóstico).</li>
 *   <li>{@link #findByTokenForUpdate(UUID)} — leitura com {@code PESSIMISTIC_WRITE},
 *       usada pelo {@code verify(UUID)} para serializar verificações
 *       concorrentes do mesmo token.</li>
 *   <li>{@link #findByAccountIdAndUsedAtIsNull(UUID)} — lista de tokens ativos
 *       de um usuário (resend marca todos como usados).</li>
 *   <li>{@link #countByAccountIdAndCreatedAtAfter(UUID, Instant)} — rate-limit
 *       de 5 emissões/hora por usuário.</li>
 *   <li>{@link #findFirstByAccountIdOrderByCreatedAtDesc(UUID)} — cooldown de
 *       60s entre reenvios consecutivos.</li>
 * </ul>
 * </p>
 *
 * <p>Visibilidade {@code public} para consumo pelo
 * {@code EmailVerificationService} (módulo {@code auth}). A
 * tabela {@code email_verification_token} é persistência do
 * subdomínio "conta" — vive em {@code accounts}; a orquestração
 * (state machine de verificação) vive em {@code auth}.</p>
 */
public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(UUID token);

    /**
     * Busca o token adquirindo lock pessimista de escrita na linha
     * ({@code SELECT ... FOR UPDATE}). Usado pelo {@code verify(UUID)} para
     * impedir que duas chamadas concorrentes do mesmo token retornem ambas
     * {@code SUCCESS}. O chamador <strong>deve</strong> estar dentro de uma
     * transação ativa ({@code @Transactional} no service).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from EmailVerificationToken t where t.token = :token")
    Optional<EmailVerificationToken> findByTokenForUpdate(@Param("token") UUID token);

    List<EmailVerificationToken> findByAccountIdAndUsedAtIsNull(UUID accountId);

    long countByAccountIdAndCreatedAtAfter(UUID accountId, Instant since);

    Optional<EmailVerificationToken> findFirstByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
