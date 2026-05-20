package dev.zayt.atrilha.auth;

/**
 * Lançada quando o reenvio de e-mail de verificação é solicitado dentro do
 * cooldown (60s) ou após o limite por hora (5) (US-006).
 *
 * <p>O campo {@code retryAfterSeconds} é entregue ao usuário (UX spec §3.3 —
 * cooldown visível). O limite/hora não é revelado (decisão UX 3.3).</p>
 */
class EmailResendRateLimitedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long retryAfterSeconds;

    EmailResendRateLimitedException(long retryAfterSeconds) {
        super("Reenvio em cooldown — aguarde " + retryAfterSeconds + "s");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
