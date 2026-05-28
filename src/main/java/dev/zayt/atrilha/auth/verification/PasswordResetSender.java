package dev.zayt.atrilha.auth.verification;

import java.util.UUID;

/**
 * Interface package-private para envio de e-mail de password reset.
 * Implementação no-op nesta slice (US-008-a); implementação real com JavaMail
 * vem em US-008-b no pacote {@code dev.zayt.atrilha.notifications}.
 */
interface PasswordResetSender {

    /**
     * Envia e-mail de recuperação de senha para o account identificado.
     * @param accountId  UUID da conta destino
     * @param tokenUuid  UUID do token de uso único (1h TTL)
     */
    void send(UUID accountId, UUID tokenUuid);

    /**
     * @return {@code true} se o sender está habilitado para envio real
     */
    boolean isEnabled();
}
