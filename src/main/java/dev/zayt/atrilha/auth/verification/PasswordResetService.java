package dev.zayt.atrilha.auth.verification;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.auth.domain.PasswordResetResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final PasswordResetTokenRepository repository;
    private final Clock clock;

    public PasswordResetService(
            PasswordResetTokenRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Emite um novo token de password reset para a account.
     * Invalida todos os tokens pendentes anteriores (marca {@code used_at}).
     * <p>Não envia e-mail — essa responsabilidade é do controller que orquestra
     * o fluxo.</p>
     *
     * @param account  conta alvo (não null)
     * @return UUID do token emitido
     */
    @Transactional
    public UUID issueToken(Account account) {
        // Invalida tokens pendentes anteriores
        List<PasswordResetToken> pending = repository.findByAccountIdAndUsedAtIsNull(account.getId());
        Instant now = clock.instant();
        for (PasswordResetToken t : pending) {
            t.setUsedAt(now);
        }
        if (!pending.isEmpty()) {
            repository.saveAll(pending);
        }

        // Emite novo token com TTL de 1h
        UUID tokenUuid = UUID.randomUUID();
        PasswordResetToken entity = new PasswordResetToken();
        entity.setId(UUID.randomUUID());
        entity.setAccountId(account.getId());
        entity.setToken(tokenUuid);
        entity.setExpiresAt(now.plus(TOKEN_TTL));
        entity.setUsedAt(null);
        entity.setCreatedAt(now);
        repository.save(entity);

        return tokenUuid;
    }

    /**
     * Verifica e consome um token de password reset.
     * Usa lock pessimista para evitar condições de corrida em uso concorrente.
     *
     * @param tokenValue  UUID do token a verificar (não null)
     * @return resultado da verificação
     */
    @Transactional
    public PasswordResetResult verify(UUID tokenValue) {
        PasswordResetToken entity = repository.findByTokenForUpdate(tokenValue)
                .orElse(null);

        if (entity == null) {
            return PasswordResetResult.EXPIRED_OR_INVALID;
        }

        if (entity.getUsedAt() != null) {
            return PasswordResetResult.ALREADY_USED;
        }

        if (clock.instant().isAfter(entity.getExpiresAt())) {
            return PasswordResetResult.EXPIRED_OR_INVALID;
        }

        // Token válido — consome
        entity.setUsedAt(clock.instant());
        repository.save(entity);
        return PasswordResetResult.SUCCESS;
    }

    /**
     * Marca um token como consumido (chamado após nova senha ser definida).
     * Idempotente: se já consumido, não faz nada. Se inexistente, não lança exceção.
     *
     * @param tokenValue  UUID do token a consumir (não null)
     */
    @Transactional
    public void consume(UUID tokenValue) {
        repository.findByTokenForUpdate(tokenValue).ifPresent(entity -> {
            if (entity.getUsedAt() == null) {
                entity.setUsedAt(clock.instant());
            }
        });
    }
}
