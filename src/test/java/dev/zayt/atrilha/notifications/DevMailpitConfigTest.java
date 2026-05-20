package dev.zayt.atrilha.notifications;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test do perfil {@code dev} para a configuração de SMTP local
 * (Mailpit via {@code docker-compose}).
 *
 * <p>O Codificador escolheu Mailpit em {@code localhost:1025} (SMTP) +
 * {@code 8025} (UI Web). Esses valores são contratuais com o
 * {@code docker-compose.yml} — quem subir o ambiente dev espera bater nessas
 * portas. Se mudarem sem atualizar o docker-compose, o ambiente dev quebra
 * silenciosamente (e-mails caem no vazio).</p>
 *
 * <p>Lê o {@code application-dev.properties} diretamente do classpath em vez
 * de subir contexto Spring com profile {@code dev} (que tentaria conexão real
 * com Postgres em {@code localhost:5432}).</p>
 */
class DevMailpitConfigTest {

    @Test
    void devProfile_smtpPointsToMailpitOnLocalhost1025() throws IOException {
        Properties props = loadDevProperties();

        assertThat(props.getProperty("spring.mail.host"))
                .as("ambiente dev fala SMTP com mailpit local")
                .isEqualTo("localhost");
        assertThat(props.getProperty("spring.mail.port"))
                .as("porta SMTP do mailpit (1025) — contrato do docker-compose")
                .isEqualTo("1025");
    }

    @Test
    void devProfile_smtpAuthAndStarttlsAreDisabled() throws IOException {
        // Mailpit dev é open relay local; auth/TLS atrapalham e fazem o cliente
        // mandar dados a mais sem necessidade.
        Properties props = loadDevProperties();

        assertThat(props.getProperty("spring.mail.properties.mail.smtp.auth"))
                .as("auth desabilitado em dev (mailpit não exige)")
                .isEqualTo("false");
        assertThat(props.getProperty("spring.mail.properties.mail.smtp.starttls.enable"))
                .as("STARTTLS desabilitado em dev (mailpit não suporta)")
                .isEqualTo("false");
    }

    @Test
    void devProfile_baseUrlAndFromAddressArePresent() throws IOException {
        Properties props = loadDevProperties();

        assertThat(props.getProperty("atrilha.base-url"))
                .as("base-url necessária para montar o link /verify-email no e-mail")
                .isNotBlank();
        assertThat(props.getProperty("atrilha.mail.from"))
                .as("from address necessária para identificar o remetente")
                .isNotBlank();
    }

    private Properties loadDevProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream in = DevMailpitConfigTest.class.getClassLoader()
                .getResourceAsStream("application-dev.properties")) {
            assertThat(in).as("application-dev.properties deve existir no classpath").isNotNull();
            props.load(in);
        }
        return props;
    }
}
