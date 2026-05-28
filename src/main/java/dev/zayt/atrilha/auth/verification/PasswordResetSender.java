package dev.zayt.atrilha.auth.verification;

import java.util.UUID;

/**
 * Interface para envio de e-mail de password reset.
 * Implementação no-op nesta slice (US-008-a); implementação real com JavaMail
 * vem em US-008-b no pacote {@code dev.zayt.atrilha.notifications}.
 */
public interface PasswordResetSender {

    /**
     * Envia e-mail de recuperação de senha com dados prontos para renderização.
     * @param toEmail    endereço de destino
     * @param nickname   apelido (nickname) do usuário — pode ser vazio
     * @param token      UUID do token de uso único (1h TTL)
     */
    void sendReset(String toEmail, String nickname, UUID token);

    /**
     * @return {@code true} se o sender está habilitado para envio real
     */
    boolean isEnabled();
}
