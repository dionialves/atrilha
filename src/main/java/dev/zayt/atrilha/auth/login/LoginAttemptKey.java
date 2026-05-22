package dev.zayt.atrilha.auth.login;

import java.util.Locale;
import java.util.Objects;

/**
 * Chave imutável para o mapa de tentativas de login.
 *
 * <p>Normaliza o e-mail (trim + lowercase) para que "Ana@X.com" e
 * "ana@x.com" sejam a mesma chave. O IP é preservado como recebido.</p>
 */
public record LoginAttemptKey(String emailNormalized, String ip) {

    /**
     * Factory que normaliza o e-mail antes de criar a chave.
     */
    public static LoginAttemptKey of(String email, String ip) {
        return new LoginAttemptKey(
                Objects.requireNonNull(email).trim().toLowerCase(Locale.ROOT),
                Objects.requireNonNull(ip));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginAttemptKey that)) return false;
        return emailNormalized.equals(that.emailNormalized)
                && ip.equals(that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailNormalized, ip);
    }
}
