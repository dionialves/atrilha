package dev.zayt.atrilha.auth.verification;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.auth.domain.PasswordResetResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestrador da recuperação de senha (US-008-a).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Emitir tokens UUID v4 com validade de 1h ({@link #issueToken(Account)}).</li>
 *   <li>Verificar tokens recebidos via URL, mapeando para {@link PasswordResetResult}
 *       ({@link #verify(UUID)}).</li>
 *   <li>Consumir tokens explicitamente ({@link #consume(UUID)}).</li>
 * </ul>
 * </p>
 *
 * <p>Decisões registradas:
 * <ul>
 *   <li>Token UUID v4 persistido (não JWT) — revogação imediata + auditoria SQL.</li>
 *   <li>TTL 1h — padrão de recuperação de senha (vs. 24h da verificação de e-mail).</li>
 *   <li>{@code PasswordResetResult} separado de {@code VerificationResult} —
 *       outcomes distintos, mas segue o mesmo padrão de unificação
 *       (EXPIRED_OR_INVALID).</li>
 * </ul>
 * </p>
 *
 * <p>Visibilidade package-private. Outros módulos chamam via Spring DI.</p>
 */
@Service
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetSender emailSender;
    private final Clock clock;

    PasswordResetService(PasswordResetTokenRepository tokenRepository,
                         PasswordResetSender emailSender,
                         Clock clock) {
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    /**
     * Emite um novo token de recuperação para a conta, invalidando tokens
     * pendentes anteriores. Retorna o UUID do token gerado.
     */
    @Transactional
    public UUID issueToken(Account account) {
        Instant now = clock.instant();
        invalidatePendingTokens(account.getId(), now);

        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setAccountId(account.getId());
        token.setToken(UUID.randomUUID());
        token.setExpiresAt(now.plus(TOKEN_TTL));
        token.setCreatedAt(now);
        tokenRepository.save(token);

        if (emailSender.isEnabled()) {
            emailSender.send(account.getId(), token.getToken());
        }
        return token.getToken();
    }

    /**
     * Verifica um token recebido via URL e marca-o como consumido.
     */
    @Transactional
    public PasswordResetResult verify(UUID tokenValue) {
        if (tokenValue == null) {
            return PasswordResetResult.EXPIRED_OR_INVALID;
        }

        // Lock pessimista de linha: duas chamadas concorrentes do mesmo
        // token são serializadas — a segunda só lê depois que a primeira
        // commitar (e então vê used_at != null, retornando ALREADY_USED).
        Optional<PasswordResetToken> opt = tokenRepository.findByTokenForUpdate(tokenValue);
        if (opt.isEmpty()) {
            return PasswordResetResult.EXPIRED_OR_INVALID;
        }

        PasswordResetToken token = opt.get();

        if (token.getUsedAt() != null) {
            return PasswordResetResult.ALREADY_USED;
        }

        Instant now = clock.instant();
        if (token.getExpiresAt().isBefore(now)) {
            return PasswordResetResult.EXPIRED_OR_INVALID;
        }

        // Marca o token como consumido.
        token.setUsedAt(now);
        tokenRepository.save(token);

        return PasswordResetResult.SUCCESS;
    }

    /**
     * Consome um token de recuperação explicitamente (marca como usado).
     *
     * <p>Idempotente: se o token já foi consumido ou não existe, retorna
     * {@code false} sem lançar exceção. Usa lock pessimista para serializar
     * chamadas concorrentes.</p>
     *
     * @return {@code true} se o token foi consumido com sucesso, {@code false}
     *         se já estava consumido ou não existe.
     */
    @Transactional
    public boolean consume(UUID tokenValue) {
        Optional<PasswordResetToken> opt = tokenRepository.findByTokenForUpdate(tokenValue);
        if (opt.isEmpty()) {
            return false;
        }

        PasswordResetToken token = opt.get();
        if (token.getUsedAt() != null) {
            return false;
        }

        token.setUsedAt(clock.instant());
        tokenRepository.save(token);
        return true;
    }

    /**
     * Marca todos os tokens pendentes (used_at IS NULL) como usados — chamado
     * antes de emitir um novo token, garantindo que apenas o último vale.
     */
    private void invalidatePendingTokens(UUID accountId, Instant now) {
        List<PasswordResetToken> pending =
                tokenRepository.findByAccountIdAndUsedAtIsNull(accountId);
        for (PasswordResetToken t : pending) {
            t.setUsedAt(now);
        }
        if (!pending.isEmpty()) {
            tokenRepository.saveAll(pending);
        }
    }
}
