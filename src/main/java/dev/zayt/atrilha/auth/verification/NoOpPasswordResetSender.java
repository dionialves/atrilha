package dev.zayt.atrilha.auth.verification;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub no-op para {@link PasswordResetSender} (US-008-a).
 *
 * <p>Implementação padrão — sem envio real de e-mail. Em testes, o bean
 * é substituído por uma versão com {@code @Primary} via {@code TestBeans}.</p>
 */
@Component
@Primary
class NoOpPasswordResetSender implements PasswordResetSender {

    @Override
    public void send(UUID accountId, UUID tokenUuid) {
        // noop — testes não validam conteúdo do e-mail aqui.
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
