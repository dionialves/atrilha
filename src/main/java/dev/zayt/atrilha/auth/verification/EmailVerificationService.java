package dev.zayt.atrilha.auth.verification;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.repository.AccountProfileLookup;
import dev.zayt.atrilha.accounts.repository.AccountReader;
import dev.zayt.atrilha.auth.verification.EmailVerificationToken;
import dev.zayt.atrilha.auth.verification.EmailVerificationTokenRepository;
import dev.zayt.atrilha.auth.domain.VerificationResult;
import dev.zayt.atrilha.auth.exception.EmailResendRateLimitedException;
import dev.zayt.atrilha.notifications.EmailVerificationSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestrador da verificação de e-mail (US-006).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Emitir tokens UUID v4 com validade de 24h ({@link #issueToken(Account)}).</li>
 *   <li>Verificar tokens recebidos via URL, mapeando para {@link VerificationResult}
 *       ({@link #verify(UUID)}).</li>
 *   <li>Reenviar e-mail respeitando cooldown de 60s e limite de 5/hora por usuário
 *       ({@link #resend(Account)}).</li>
 * </ul>
 * </p>
 *
 * <p>Decisões registradas:
 * <ul>
 *   <li>Token UUID v4 persistido (não JWT) — revogação imediata + auditoria SQL.</li>
 *   <li>TTL 24h vs 1h da recuperação de senha (US-008) — padrão de uso real.</li>
 *   <li>Rate-limit em SQL via {@code created_at} (não in-memory) — sobrevive a
 *       restart do app, multi-instance friendly.</li>
 *   <li>EXPIRED_OR_INVALID unifica "não-existe" e "expirado" — UX spec §5.3.</li>
 * </ul>
 * </p>
 *
 * <p>Visibilidade package-private. Outros módulos chamam via Spring DI.</p>
 */
@Service
public class EmailVerificationService {

    static final Duration TOKEN_TTL = Duration.ofHours(24);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int RESEND_MAX_PER_HOUR = 5;

    private final EmailVerificationTokenRepository tokenRepository;
    private final AccountReader accountReader;
    private final AccountProfileLookup profileLookup;
    private final EmailVerificationSender emailSender;
    private final Clock clock;

    EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                             AccountReader accountReader,
                             AccountProfileLookup profileLookup,
                             EmailVerificationSender emailSender,
                             Clock clock) {
        this.tokenRepository = tokenRepository;
        this.accountReader = accountReader;
        this.profileLookup = profileLookup;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    /**
     * Emite um novo token de verificação para a conta, invalidando tokens
     * pendentes anteriores. Retorna o UUID do token gerado.
     */
    @Transactional
    public UUID issueToken(Account account) {
        Instant now = clock.instant();
        invalidatePendingTokens(account.getId(), now);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setId(UUID.randomUUID());
        token.setAccountId(account.getId());
        token.setToken(UUID.randomUUID());
        token.setExpiresAt(now.plus(TOKEN_TTL));
        token.setCreatedAt(now);
        tokenRepository.save(token);
        return token.getToken();
    }

    /**
     * Verifica um token recebido via URL e atualiza o estado da conta.
     */
    @Transactional
    public VerificationResult verify(UUID tokenValue) {
        if (tokenValue == null) {
            return VerificationResult.EXPIRED_OR_INVALID;
        }
        // Lock pessimista de linha: duas chamadas concorrentes do mesmo
        // token são serializadas — a segunda só lê depois que a primeira
        // commitar (e então vê used_at != null, retornando ALREADY_USED).
        Optional<EmailVerificationToken> opt = tokenRepository.findByTokenForUpdate(tokenValue);
        if (opt.isEmpty()) {
            return VerificationResult.EXPIRED_OR_INVALID;
        }
        EmailVerificationToken token = opt.get();
        if (token.getUsedAt() != null) {
            return VerificationResult.ALREADY_USED;
        }
        Instant now = clock.instant();
        if (token.getExpiresAt().isBefore(now)) {
            return VerificationResult.EXPIRED_OR_INVALID;
        }

        Optional<Account> accountOpt = accountReader.findById(token.getAccountId());
        if (accountOpt.isEmpty()) {
            return VerificationResult.EXPIRED_OR_INVALID;
        }
        Account account = accountOpt.get();

        // Marca o token como usado independentemente do estado da conta — evita
        // que o mesmo token possa ser reapresentado depois (defesa em profundidade).
        token.setUsedAt(now);
        tokenRepository.save(token);

        if (account.getEmailVerifiedAt() != null) {
            // Idempotência: conta já estava verificada por outra via — não sobrescreve
            // o timestamp original; sinaliza ALREADY_USED ao chamador (mesma tela UX).
            return VerificationResult.ALREADY_USED;
        }

        accountReader.markEmailVerifiedAt(
                account.getId(), OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
        return VerificationResult.SUCCESS;
    }

    /**
     * Reenvia o e-mail de verificação para a conta, respeitando cooldown
     * (60s) e limite por hora (5).
     *
     * @throws EmailResendRateLimitedException se o cooldown ou o limite/hora
     *         tiver sido atingido. O campo {@code retryAfterSeconds} expõe o
     *         tempo restante; o limite/hora não é revelado ao chamador
     *         (UX spec §3.3).
     */
    @Transactional
    public void resend(Account account) {
        Instant now = clock.instant();

        // 1) Limite por hora — verificado primeiro porque expõe menos sinal de
        //    estado ao chamador (não sabemos qual foi a hora exata do último
        //    bloqueio, apenas que esperar resolve).
        long inLastHour = tokenRepository.countByAccountIdAndCreatedAtAfter(
                account.getId(), now.minus(Duration.ofHours(1)));
        if (inLastHour >= RESEND_MAX_PER_HOUR) {
            Optional<EmailVerificationToken> oldestInWindow =
                    tokenRepository.findFirstByAccountIdOrderByCreatedAtDesc(account.getId());
            long secondsUntilFree = oldestInWindow
                    .map(t -> Duration.between(now, t.getCreatedAt().plus(Duration.ofHours(1))).getSeconds())
                    .orElse(RESEND_COOLDOWN.getSeconds());
            throw new EmailResendRateLimitedException(Math.max(secondsUntilFree, RESEND_COOLDOWN.getSeconds()));
        }

        // 2) Cooldown — 60s entre tentativas consecutivas.
        Optional<EmailVerificationToken> last =
                tokenRepository.findFirstByAccountIdOrderByCreatedAtDesc(account.getId());
        if (last.isPresent()) {
            Duration sinceLast = Duration.between(last.get().getCreatedAt(), now);
            if (sinceLast.compareTo(RESEND_COOLDOWN) < 0) {
                long retryAfter = RESEND_COOLDOWN.minus(sinceLast).getSeconds();
                // Mínimo 1 segundo para o usuário ver o aviso.
                throw new EmailResendRateLimitedException(Math.max(retryAfter, 1));
            }
        }

        UUID newToken = issueToken(account);
        String nickname = profileLookup.findNickname(account.getId()).orElse("");
        emailSender.sendVerification(account.getEmail(), nickname, newToken);
    }

    /**
     * Marca todos os tokens pendentes (used_at IS NULL) como usados — chamado
     * antes de emitir um novo token, garantindo que apenas o último vale.
     */
    private void invalidatePendingTokens(UUID accountId, Instant now) {
        List<EmailVerificationToken> pending =
                tokenRepository.findByAccountIdAndUsedAtIsNull(accountId);
        for (EmailVerificationToken t : pending) {
            t.setUsedAt(now);
        }
        if (!pending.isEmpty()) {
            tokenRepository.saveAll(pending);
        }
    }
}
