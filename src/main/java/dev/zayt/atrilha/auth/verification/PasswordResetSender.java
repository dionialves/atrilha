package dev.zayt.atrilha.auth.verification;

import java.util.UUID;

/**
 * Envio de e-mail de recuperação de senha (US-008-a).
 *
 * <p>Interface package-private para injeção no service. A implementação
 * real (SMTP) será adicionada em US-008-b; por enquanto, {@code NoOp}
 * serve como stub.</p>
 */
interface PasswordResetSender {

    /**
     * Envia o e-mail de recuperação de senha para a conta identificada por
     * {@code accountId}, contendo o link com {@code tokenUuid}.
     */
    void send(UUID accountId, UUID tokenUuid);

    /**
     * Retorna {@code true} se o envio de e-mails está habilitado.
     */
    boolean isEnabled();
}
