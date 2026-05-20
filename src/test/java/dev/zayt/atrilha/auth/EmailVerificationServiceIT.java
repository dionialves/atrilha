package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.Account;
import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.accounts.EmailVerificationToken;
import dev.zayt.atrilha.accounts.EmailVerificationTokenRepository;
import dev.zayt.atrilha.notifications.RecordedEmail;
import dev.zayt.atrilha.notifications.RecordingEmailSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração do {@link EmailVerificationService} — emissão e verificação
 * de tokens (US-006).
 *
 * <p>Cobre os 4 outcomes da verificação (SUCCESS / NOT_FOUND / EXPIRED /
 * ALREADY_USED), idempotência de re-verificação de usuário já verificado,
 * e validade do TTL de 24h.</p>
 *
 * <p>Substitui o {@code EmailSender} real por um {@link RecordingEmailSender}
 * para isolar o service do mailer JavaMail (testado separadamente).</p>
 */
@Testcontainers
@SpringBootTest(classes = { AtrilhaApplication.class, EmailVerificationServiceIT.TestBeans.class },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@DirtiesContext
class EmailVerificationServiceIT {

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

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse("2026-05-19T12:00:00Z"),
                    ZoneOffset.UTC);
        }
    }

    @Autowired
    EmailVerificationService service;

    @Autowired
    EmailVerificationTokenRepository tokenRepository;

    @Autowired
    EntityManager em;

    @Autowired
    RecordingEmailSender mailer;

    private Account persistedAccount(String email) {
        Account a = AccountTestFactory.newAdolescent(email);
        em.persist(a);
        em.flush();
        return a;
    }

    @BeforeEach
    void cleanMailer() {
        mailer.clear();
    }

    // ---- Suite 2 — issueToken ----

    @Test
    @Transactional
    void issueToken_persistsTokenWith24hExpiry_andUsedAtNull() {
        Account a = persistedAccount("issue1@example.com");

        UUID token = service.issueToken(a);
        em.flush();
        em.clear();

        EmailVerificationToken persisted = tokenRepository.findByToken(token).orElseThrow();
        assertThat(persisted.getAccountId()).isEqualTo(a.getId());
        assertThat(persisted.getUsedAt()).isNull();
        assertThat(persisted.getExpiresAt())
                .isAfter(Instant.parse("2026-05-19T12:00:00Z").plus(23, ChronoUnit.HOURS).minusSeconds(1))
                .isBefore(Instant.parse("2026-05-19T12:00:00Z").plus(24, ChronoUnit.HOURS).plusSeconds(1));
    }

    // ---- Suite 2 — verify ----

    @Test
    @Transactional
    void verify_validToken_marksEmailVerifiedAndTokenUsed_returnsSuccess() {
        Account a = persistedAccount("verif1@example.com");
        UUID token = service.issueToken(a);
        em.flush();
        em.clear();

        VerificationResult result = service.verify(token);
        em.flush();
        em.clear();

        assertThat(result).isEqualTo(VerificationResult.SUCCESS);
        EmailVerificationToken used = tokenRepository.findByToken(token).orElseThrow();
        assertThat(used.getUsedAt()).isNotNull();
        Account refreshed = em.find(Account.class, a.getId());
        assertThat(refreshed.getEmailVerifiedAt()).isNotNull();
    }

    @Test
    @Transactional
    void verify_alreadyUsedToken_returnsAlreadyUsed() {
        Account a = persistedAccount("verif2@example.com");
        UUID token = service.issueToken(a);
        em.flush();
        // primeira verificação → success
        service.verify(token);
        em.flush();
        em.clear();

        VerificationResult result = service.verify(token);

        assertThat(result).isEqualTo(VerificationResult.ALREADY_USED);
    }

    @Test
    @Transactional
    void verify_expiredToken_returnsExpired() {
        Account a = persistedAccount("verif3@example.com");
        EmailVerificationToken expired = new EmailVerificationToken();
        expired.setId(UUID.randomUUID());
        expired.setAccountId(a.getId());
        expired.setToken(UUID.randomUUID());
        expired.setExpiresAt(Instant.parse("2026-05-19T12:00:00Z").minus(1, ChronoUnit.HOURS));
        expired.setCreatedAt(Instant.parse("2026-05-19T12:00:00Z").minus(25, ChronoUnit.HOURS));
        tokenRepository.saveAndFlush(expired);
        em.clear();

        VerificationResult result = service.verify(expired.getToken());

        assertThat(result).isEqualTo(VerificationResult.EXPIRED_OR_INVALID);
        // expired tokens não devem marcar email_verified_at
        Account refreshed = em.find(Account.class, a.getId());
        assertThat(refreshed.getEmailVerifiedAt()).isNull();
    }

    @Test
    @Transactional
    void verify_unknownToken_returnsExpiredOrInvalid() {
        VerificationResult result = service.verify(UUID.randomUUID());
        assertThat(result).isEqualTo(VerificationResult.EXPIRED_OR_INVALID);
    }

    @Test
    @Transactional
    void verify_alreadyVerifiedUser_marksTokenUsedButDoesNotOverwriteTimestamp() {
        Account a = persistedAccount("verif4@example.com");
        OffsetDateTime originalVerifiedAt =
                OffsetDateTime.of(2026, 5, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        a.setEmailVerifiedAt(originalVerifiedAt);
        em.merge(a);
        em.flush();
        UUID token = service.issueToken(a);
        em.flush();
        em.clear();

        VerificationResult result = service.verify(token);
        em.flush();
        em.clear();

        // Comportamento: token é marcado, mas o timestamp original NÃO é sobrescrito.
        // Retorno expressa idempotência aceitável — ALREADY_USED é o sinal canônico
        // de "nada a fazer" (UX exibe a tela de "já foi confirmado").
        assertThat(result).isEqualTo(VerificationResult.ALREADY_USED);
        EmailVerificationToken used = tokenRepository.findByToken(token).orElseThrow();
        assertThat(used.getUsedAt()).isNotNull();
        Account refreshed = em.find(Account.class, a.getId());
        assertThat(refreshed.getEmailVerifiedAt())
                .as("não deve sobrescrever o timestamp original")
                .isEqualTo(originalVerifiedAt);
    }
}
