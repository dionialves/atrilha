package dev.zayt.atrilha.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propriedades de rate-limit para o formulário de login.
 *
 * <p>Prefixo {@code atrilha.auth.login}. Defaults conservadores para o MVP:
 * 5 tentativas em janela de 15 minutos, bloqueio de 15 minutos.</p>
 */
@ConfigurationProperties(prefix = "atrilha.auth.login")
public record LoginRateLimitProperties(
        int maxAttempts,
        Duration attemptWindow,
        Duration blockDuration) {

    public static final int DEFAULT_MAX_ATTEMPTS = 5;
    public static final Duration DEFAULT_ATTEMPT_WINDOW = Duration.ofMinutes(15);
    public static final Duration DEFAULT_BLOCK_DURATION = Duration.ofMinutes(15);

    public LoginRateLimitProperties {
        if (maxAttempts <= 0) maxAttempts = DEFAULT_MAX_ATTEMPTS;
        if (attemptWindow == null || attemptWindow.isZero() || attemptWindow.isNegative()) {
            attemptWindow = DEFAULT_ATTEMPT_WINDOW;
        }
        if (blockDuration == null || blockDuration.isZero() || blockDuration.isNegative()) {
            blockDuration = DEFAULT_BLOCK_DURATION;
        }
    }

    public static LoginRateLimitProperties defaults() {
        return new LoginRateLimitProperties(
                DEFAULT_MAX_ATTEMPTS,
                DEFAULT_ATTEMPT_WINDOW,
                DEFAULT_BLOCK_DURATION);
    }
}
