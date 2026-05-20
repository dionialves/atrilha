package dev.zayt.atrilha.notifications;

import java.util.UUID;

/**
 * Contrato de envio do e-mail de verificação (US-006 / RF-E1-07).
 *
 * <p>Mantida {@code public} para que o módulo {@code auth} consuma a
 * interface sem importar a implementação concreta JavaMail. Implementações
 * vivem dentro de {@code notifications}.</p>
 *
 * <p>O parâmetro {@code token} é segredo de uso único — logs em
 * implementações devem omiti-lo (PRD §11.8).</p>
 */
public interface EmailVerificationSender {

    /**
     * Envia o e-mail de verificação para o destinatário.
     *
     * @param toEmail e-mail de destino (já normalizado pelo service).
     * @param nickname apelido a saudar na mensagem.
     * @param token UUID a ser embutido no link {@code /verify-email?token=}.
     */
    void sendVerification(String toEmail, String nickname, UUID token);
}
