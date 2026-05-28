package dev.zayt.atrilha.notifications;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Testes unitários de {@link JavaMailPasswordResetSender} com servidor SMTP em memória (GreenMail).
 * Valida renderização de templates, cabeçalhos e tratamento de borda.
 */
class JavaMailPasswordResetSenderTest {

    static GreenMail greenMail;

    private static final String FROM = "atrilha@mail.atrilha.dev";
    private static final String BASE_URL = "http://localhost:8084";

    private JavaMailPasswordResetSender sender;

    @BeforeAll
    static void startGreenMail() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @AfterAll
    static void stopGreenMail() {
        if (greenMail != null) greenMail.stop();
    }

    @AfterEach
    void resetMail() {
        greenMail.reset();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        var mailSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        mailSender.setHost("127.0.0.1");
        mailSender.setPort(ServerSetupTest.SMTP.getPort());
        mailSender.setProtocol("smtp");
        sender = new JavaMailPasswordResetSender(mailSender, FROM, BASE_URL);
    }

    @Test
    void sendReset_rendersWithCorrectVariables() throws Exception {
        UUID token = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        sender.sendReset("ana@example.com", "Ana", token);

        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage msg = greenMail.getReceivedMessages()[0];

        String htmlBody = extractPart(msg, "text/html");

        assertThat(htmlBody)
                .as("nickname renderizado")
                .contains("<strong>Ana</strong>");
        assertThat(htmlBody)
                .as("URL de reset com token")
                .contains("http://localhost:8084/reset-senha?token=a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        assertThat(htmlBody)
                .as("CTA presente")
                .contains("Redefinir minha senha");
        assertThat(htmlBody)
                .as("TTL correto (1 hora, não 24h)")
                .contains("vale por 1 hora");
    }

    @Test
    void sendReset_sendsMimeMessageWithCorrectHeaders() throws Exception {
        UUID token = UUID.fromString("11111111-2222-3333-4444-555555555555");

        sender.sendReset("ana@example.com", "Ana", token);

        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage msg = greenMail.getReceivedMessages()[0];

        assertThat(msg.getSubject())
                .isEqualTo("Redefinição de senha no atrilha");
        assertThat(msg.getFrom()[0].toString())
                .isEqualTo("atrilha@mail.atrilha.dev");
        assertThat(msg.getAllRecipients()[0].toString())
                .isEqualTo("ana@example.com");

        // Verifica que o content é multipart (HTML + plain)
        assertThat(msg.getContent()).isInstanceOf(Multipart.class);
    }

    @Test
    void sendReset_handlesBlankNickname() throws Exception {
        UUID token = UUID.fromString("99999999-8888-7777-6666-555555555555");

        assertThatCode(() -> sender.sendReset("test@example.com", "", token))
                .as("não lança exceção com nickname vazio")
                .doesNotThrowAnyException();

        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage msg = greenMail.getReceivedMessages()[0];

        String htmlBody = extractPart(msg, "text/html");
        assertThat(htmlBody)
                .as("nickname vazio renderizado como string vazia")
                .contains("<strong></strong>");
    }

    private String extractPart(MimeMessage msg, String contentTypeFragment) throws Exception {
        String result = findPart(msg.getContent(), contentTypeFragment);
        if (result == null) {
            throw new AssertionError("part " + contentTypeFragment + " não encontrado");
        }
        return result;
    }

    /** Walk recursivamente a árvore multipart procurando a primeira part com o content-type pedido. */
    private static String findPart(Object content, String contentTypeFragment) throws Exception {
        if (content instanceof Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                String ct = bp.getContentType().toLowerCase();
                if (ct.contains(contentTypeFragment)) {
                    return bp.getContent().toString();
                }
                String nested = findPart(bp.getContent(), contentTypeFragment);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
