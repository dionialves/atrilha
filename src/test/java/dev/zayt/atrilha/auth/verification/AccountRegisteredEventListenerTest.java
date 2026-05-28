package dev.zayt.atrilha.auth.verification;

import dev.zayt.atrilha.testsupport.AbstractSpringPostgresIT;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.domain.AccountRegisteredEvent;
import dev.zayt.atrilha.notifications.RecordedEmail;
import dev.zayt.atrilha.notifications.RecordingEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para {@link AccountRegisteredEventListener} focados
 * no nome usado no e-mail de verificação por tipo de conta (US-003).
 *
 * <p>Valida que o e-mail de verificação usa o full_name do GuardianProfile
 * para contas GUARDIAN, e mantém o nickname para ADOLESCENT (sem regressão).</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, AccountRegisteredEventListenerTest.TestBeans.class },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "atrilha.auth.seed.enabled=false"
        })
@ActiveProfiles("test")
class AccountRegisteredEventListenerTest extends AbstractSpringPostgresIT {

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingEmailSender mailer;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager txManager;

    TransactionTemplate tx;

    @BeforeEach
    void cleanMailer() {
        mailer.clear();
        tx = new TransactionTemplate(txManager);
    }

    // ---- E-mail de verificação com nome correto por tipo ----

    @Test
    @DisplayName("sendsVerificationEmailWithFullNameForGuardian")
    void sendsVerificationEmailWithFullNameForGuardian() {
        UUID accountId = UUID.randomUUID();
        String email = "ana-g-" + System.nanoTime() + "@teste.com";

        insertAccount(accountId, "GUARDIAN", email);
        insertGuardianProfile(accountId, "Ana Silva");

        publishEventInTx(accountId);

        assertThat(mailer.recorded()).hasSize(1);
        RecordedEmail emailRecord = mailer.recorded().get(0);
        assertThat(emailRecord.toEmail()).isEqualTo(email);
        assertThat(emailRecord.nickname()).isEqualTo("Ana Silva");
    }

    @Test
    @DisplayName("sendsVerificationEmailWithNicknameForAdolescent")
    void sendsVerificationEmailWithNicknameForAdolescent() {
        UUID accountId = UUID.randomUUID();
        String email = "pedro-a-" + System.nanoTime() + "@teste.com";

        insertAccount(accountId, "ADOLESCENT", email);
        insertAdolescentProfile(accountId, "Pedro");

        publishEventInTx(accountId);

        assertThat(mailer.recorded()).hasSize(1);
        RecordedEmail emailRecord = mailer.recorded().get(0);
        assertThat(emailRecord.toEmail()).isEqualTo(email);
        assertThat(emailRecord.nickname()).isEqualTo("Pedro");
    }

    // ---- Helpers ----

    private void insertAccount(UUID id, String type, String email) {
        jdbcTemplate.update(
                """
                        INSERT INTO accounts (id, type, email, password_hash, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                id, type, email, "$2b$12$" + "x".repeat(53), OffsetDateTime.now());
    }

    private void insertGuardianProfile(UUID accountId, String fullName) {
        jdbcTemplate.update(
                """
                        INSERT INTO guardian_profiles (account_id, full_name)
                        VALUES (?, ?)
                        """,
                accountId, fullName);
    }

    private void insertAdolescentProfile(UUID accountId, String nickname) {
        jdbcTemplate.update(
                """
                        INSERT INTO adolescent_profiles (account_id, nickname, birth_date, timezone)
                        VALUES (?, ?, ?, ?)
                        """,
                accountId, nickname, java.time.LocalDate.of(2013, 6, 15), "America/Sao_Paulo");
    }

    private void publishEventInTx(UUID accountId) {
        tx.executeWithoutResult(status -> publisher.publishEvent(new AccountRegisteredEvent(accountId)));
    }
}
