package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.Account;
import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.accounts.EmailVerificationTokenRepository;
import dev.zayt.atrilha.notifications.RecordedEmail;
import dev.zayt.atrilha.notifications.RecordingEmailSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração do listener {@code AccountRegisteredEventListener} (US-006 §M).
 *
 * <p>Garante:
 * <ul>
 *   <li>Após a transação que publica {@link AccountRegisteredEvent} commitar,
 *       exatamente 1 token é emitido e 1 e-mail é enviado.</li>
 *   <li>Se a transação que publica o evento sofrer rollback, nenhum e-mail é
 *       enviado (decisão arquitetural: handler {@code AFTER_COMMIT}).</li>
 * </ul>
 * </p>
 *
 * <p>Por respeito à fronteira de pacote (PRD §9.3), o teste não chama
 * {@code RegisterAdolescentService} (package-private em {@code accounts}).
 * Em vez disso, persiste a conta com {@code AccountTestFactory} e publica o
 * evento manualmente — o efeito sobre o listener é o mesmo, e a integração
 * "register → publishEvent" é coberta pelo {@code AdolescentRegistrationControllerIT}.</p>
 */
@Testcontainers
@SpringBootTest(classes = { AtrilhaApplication.class, AccountRegisteredEventListenerIT.TestBeans.class },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@DirtiesContext
class AccountRegisteredEventListenerIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("atrilha")
                    .withUsername("atrilha")
                    .withPassword("atrilha");

    @DynamicPropertySource
    static void registerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    @Autowired
    EmailVerificationTokenRepository tokenRepository;

    @Autowired
    RecordingEmailSender mailer;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    EntityManager em;

    TransactionTemplate tx;

    @BeforeEach
    void cleanMailer() {
        mailer.clear();
        tx = new TransactionTemplate(txManager);
    }

    private Account persistAccountInTx(String email) {
        Account a = AccountTestFactory.newAdolescent(email);
        tx.executeWithoutResult(status -> em.persist(a));
        return a;
    }

    @Test
    void eventPublished_afterCommit_sendsExactlyOneEmailAndStoresOneToken() {
        Account a = persistAccountInTx("listener-ok@example.com");

        tx.executeWithoutResult(status -> publisher.publishEvent(new AccountRegisteredEvent(a.getId())));

        var tokens = tokenRepository.findByAccountIdAndUsedAtIsNull(a.getId());
        assertThat(tokens).hasSize(1);
        assertThat(mailer.recorded()).hasSize(1);
        RecordedEmail email = mailer.recorded().get(0);
        assertThat(email.toEmail()).isEqualTo("listener-ok@example.com");
        assertThat(email.token()).isEqualTo(tokens.get(0).getToken());
    }

    @Test
    void eventPublishedThenRolledBack_emitsNoEmail() {
        Account a = persistAccountInTx("listener-rollback@example.com");
        UUID accountId = a.getId();

        tx.executeWithoutResult(status -> {
            publisher.publishEvent(new AccountRegisteredEvent(accountId));
            status.setRollbackOnly();
        });

        assertThat(mailer.recorded())
                .as("rollback não dispara e-mail (AFTER_COMMIT)")
                .isEmpty();
        assertThat(tokenRepository.findByAccountIdAndUsedAtIsNull(accountId))
                .as("rollback não cria token")
                .isEmpty();
    }
}
