package dev.zayt.atrilha.notifications;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Substitui {@link EmailVerificationSender} por {@link RecordingEmailSender}
 * em integrações que carregam o contexto completo (Spring Boot) mas não
 * exercitam SMTP real.
 *
 * <p>Necessário a partir da US-006 porque o {@code RegisterAdolescentService}
 * publica {@code AccountRegisteredEvent} no commit, e o listener (em
 * {@code auth}) chamaria {@link JavaMailEmailVerificationSender} — que tenta
 * abrir conexão TCP em {@code localhost:2525} (host fake do {@code test}
 * profile) e gera ruído de log. Usar este @TestConfiguration deixa o teste
 * silencioso e ainda permite asserções sobre o e-mail enviado.</p>
 *
 * <p>Visibilidade pública porque é importado via {@code @Import} de outros
 * pacotes de teste.</p>
 */
@TestConfiguration
public class RecordingEmailSenderTestConfig {

    @Bean
    @Primary
    public RecordingEmailSender recordingEmailSender() {
        return new RecordingEmailSender();
    }
}
