package dev.zayt.atrilha.notifications;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import dev.zayt.atrilha.AtrilhaApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que o token nunca aparece em logs do {@code JavaMailEmailVerificationSender}
 * — requisito de privacidade (PRD §11.8 — LGPD).
 */
@SpringBootTest(classes = AtrilhaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DirtiesContext
class JavaMailVerificationSenderLogTest {

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

    ListAppender<ILoggingEvent> appender;
    Logger targetLogger;

    @BeforeEach
    void attachAppender() {
        targetLogger = (Logger) LoggerFactory.getLogger("dev.zayt.atrilha.notifications");
        appender = new ListAppender<>();
        appender.start();
        targetLogger.addAppender(appender);
        // Captura TRACE também, para garantir que nem em debug o token vaza.
        targetLogger.setLevel(Level.TRACE);
    }

    @AfterEach
    void detachAppender() {
        targetLogger.detachAppender(appender);
    }

    @Test
    void sendVerificationEmail_logsDoNotContainToken() {
        UUID token = UUID.fromString("12345678-aaaa-bbbb-cccc-1234567890ab");
        sender.sendVerification("noleak@example.com", "noleak", token);

        String allLogs = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(allLogs)
                .as("nenhum log do módulo notifications deve conter o token (PRD §11.8)")
                .doesNotContain(token.toString());
    }
}
