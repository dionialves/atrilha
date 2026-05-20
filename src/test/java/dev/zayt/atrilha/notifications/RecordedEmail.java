package dev.zayt.atrilha.notifications;

import java.util.UUID;

/**
 * Captura, em testes, os parâmetros passados ao
 * {@link EmailVerificationSender#sendVerification(String, String, UUID)}.
 *
 * <p>Visibilidade pública para uso em pacotes de teste fora de
 * {@code notifications}.</p>
 */
public record RecordedEmail(String toEmail, String nickname, UUID token) {
}
