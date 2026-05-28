package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.testsupport.AbstractSpringPostgresIT;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.AccountRegisteredEvent;
import dev.zayt.atrilha.auth.verification.EmailVerificationTokenRepository;
import dev.zayt.atrilha.auth.verification.EmailVerificationService;
import dev.zayt.atrilha.notifications.EmailVerificationSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante a resiliência do {@code AccountRegisteredEventListener} quando o
 * envio SMTP falha (decisão técnica #2 do Codificador): a conta já foi
 * persistida e o token já está no banco — o usuário pode pedir reenvio
 * pela tela. Uma falha do SMTP <strong>não pode</strong>:
 * <ul>
 *   <li>Subir como exceção pós-commit e bombardear o request original.</li>
 *   <li>Reverter ou deletar o token emitido (resend depende dele).</li>
 *   <li>Vazar o token nos logs (PRD §11.8 / LGPD).</li>
 * </ul>
 *
 * <p>Substitui o sender real por um stub que lança {@code RuntimeException};
 * publica o evento via {@code ApplicationEventPublisher} em uma transação
 * que commita; checa estado pós-commit.</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, AccountRegisteredEventListenerSmtpFailureIT.TestBeans.class },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
class AccountRegisteredEventListenerSmtpFailureIT extends AbstractSpringPostgresIT {

    /** Sender que sempre falha, capturando o token recebido para asserts de log. */
    static class ExplodingSender implements EmailVerificationSender {
        final AtomicInteger calls = new AtomicInteger(0);
        volatile UUID lastReceivedToken;

        @Override
        public void sendVerification(String toEmail, String nickname, UUID token) {
            calls.incrementAndGet();
            lastReceivedToken = token;
            throw new IllegalStateException("SMTP fora do ar (simulado)");
        }
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        ExplodingSender explodingSender() {
            return new ExplodingSender();
        }
    }

    @Autowired
    EmailVerificationTokenRepository tokenRepository;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    EntityManager em;

    @Autowired
    ExplodingSender exploding;

    ListAppender<ILoggingEvent> appender;
    Logger targetLogger;

    @BeforeEach
    void attachLogAppender() {
        targetLogger = (Logger) LoggerFactory.getLogger("dev.zayt.atrilha.auth");
        appender = new ListAppender<>();
        appender.start();
        targetLogger.addAppender(appender);
        targetLogger.setLevel(Level.TRACE);
    }

    @AfterEach
    void detachLogAppender() {
        targetLogger.detachAppender(appender);
    }

    private Account persistAccountInTx(String email) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        Account a = AccountTestFactory.newAdolescent(email);
        tx.executeWithoutResult(status -> em.persist(a));
        return a;
    }

    @Test
    void senderFailure_doesNotPropagate_andLeavesTokenPersisted() {
        Account a = persistAccountInTx("smtp-down@example.com");

        // Publicar o evento em uma transação que commita normalmente.
        // O listener (AFTER_COMMIT + REQUIRES_NEW) é invocado pós-commit;
        // a exceção do sender NÃO deve subir para esta thread.
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(status -> publisher.publishEvent(new AccountRegisteredEvent(a.getId())));

        // Sender foi chamado uma vez (e explodiu por dentro).
        assertThat(exploding.calls.get())
                .as("listener tentou enviar exatamente uma vez")
                .isEqualTo(1);

        // Token ficou persistido — usuário consegue pedir reenvio depois.
        var tokens = tokenRepository.findByAccountIdAndUsedAtIsNull(a.getId());
        assertThat(tokens)
                .as("token deve estar persistido mesmo após falha de SMTP")
                .hasSize(1);
    }

    @Test
    void senderFailure_doesNotLogToken_doesNotLogBody() {
        Account a = persistAccountInTx("smtp-leak@example.com");

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(status -> publisher.publishEvent(new AccountRegisteredEvent(a.getId())));

        // Token foi gerado pelo service; o sender capturou esse UUID e
        // explodiu. Garante que o WARN/ERROR do listener NÃO inclui o valor
        // do token (PRD §11.8) — só destinatário + cause class.
        UUID token = exploding.lastReceivedToken;
        assertThat(token).as("sender foi chamado, capturou o token").isNotNull();

        String allLogs = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (acc, msg) -> acc + "\n" + msg);

        assertThat(allLogs)
                .as("token NUNCA pode aparecer em logs (LGPD/PRD §11.8)")
                .doesNotContain(token.toString());
        // Também não pode aparecer o "corpo" — proxy razoável: a URL com token
        // (padrão /verify-email?token=...) também não deve vazar.
        assertThat(allLogs)
                .as("nem o link completo /verify-email?token=... pode vazar")
                .doesNotContain("/verify-email?token=" + token);
    }
}
