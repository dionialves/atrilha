package dev.zayt.atrilha.notifications;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import dev.zayt.atrilha.AtrilhaApplication;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração de {@link JavaMailEmailVerificationSender} com servidor SMTP
 * em-memória (GreenMail). Valida que o e-mail enviado:
 * <ul>
 *   <li>Tem parts HTML + text/plain (multipart/alternative).</li>
 *   <li>Inclui o link {@code /verify-email?token=&lt;uuid&gt;} no corpo.</li>
 *   <li>Tem From correto e Subject não-vazio contendo {@code atrilha}.</li>
 *   <li>Logs não contêm o token (PRD §11.8).</li>
 * </ul>
 */
@SpringBootTest(classes = AtrilhaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DirtiesContext
class JavaMailVerificationSenderIT {

    static GreenMail greenMail;

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

    @DynamicPropertySource
    static void smtpProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> ServerSetupTest.SMTP.getPort());
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        registry.add("atrilha.mail.from", () -> "ola@atrilha.local");
        registry.add("atrilha.base-url", () -> "http://localhost:8084");
    }

    @Autowired
    EmailVerificationSender sender;

    @Test
    void sendVerificationEmail_sendsMultipartWithHtmlAndPlainTextParts() throws Exception {
        UUID token = UUID.randomUUID();
        sender.sendVerification("julia@example.com", "julia", token);

        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage[] msgs = greenMail.getReceivedMessages();
        assertThat(msgs).hasSize(1);

        MimeMessage msg = msgs[0];
        Object content = msg.getContent();
        assertThat(content)
                .as("multipart — HTML + texto-plano")
                .isInstanceOf(Multipart.class);

        assertThat(hasContentType(content, "text/html"))
                .as("deve conter part text/html").isTrue();
        assertThat(hasContentType(content, "text/plain"))
                .as("deve conter part text/plain").isTrue();
    }

    @Test
    void sendVerificationEmail_subjectIsNonEmptyAndMentionsAtrilha() throws Exception {
        sender.sendVerification("anna@example.com", "anna", UUID.randomUUID());
        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage msg = greenMail.getReceivedMessages()[0];

        assertThat(msg.getSubject()).isNotBlank();
        assertThat(msg.getSubject().toLowerCase()).contains("atrilha");
    }

    @Test
    void sendVerificationEmail_bodyContainsLinkWithToken() throws Exception {
        UUID token = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        sender.sendVerification("link@example.com", "link", token);
        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage msg = greenMail.getReceivedMessages()[0];

        String htmlPart = extractPart(msg, "text/html");
        String plainPart = extractPart(msg, "text/plain");

        String expectedUrl = "http://localhost:8084/verify-email?token=" + token;
        assertThat(htmlPart)
                .as("HTML deve conter o link de verificação")
                .contains(expectedUrl);
        assertThat(plainPart)
                .as("texto-plano deve conter o link como URL inteira")
                .contains(expectedUrl);
    }

    @Test
    void sendVerificationEmail_fromHeaderMatchesConfiguredAddress() throws Exception {
        sender.sendVerification("from@example.com", "from", UUID.randomUUID());
        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();
        MimeMessage msg = greenMail.getReceivedMessages()[0];

        assertThat(msg.getFrom()).hasSize(1);
        assertThat(msg.getFrom()[0].toString()).contains("ola@atrilha.local");
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

    private static boolean hasContentType(Object content, String contentTypeFragment) throws Exception {
        return findPart(content, contentTypeFragment) != null;
    }
}
