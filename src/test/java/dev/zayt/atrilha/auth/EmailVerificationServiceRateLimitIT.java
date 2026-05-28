package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.testsupport.AbstractSpringPostgresIT;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.auth.verification.EmailVerificationTokenRepository;
import dev.zayt.atrilha.auth.verification.EmailVerificationService;
import dev.zayt.atrilha.auth.exception.EmailResendRateLimitedException;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rate-limit do reenvio (US-006 §3.3):
 * <ul>
 *   <li>Cooldown de 60s entre tentativas consecutivas.</li>
 *   <li>Limite de 5 reenvios por hora.</li>
 *   <li>Reenvio bem-sucedido após cooldown invalida tokens pendentes anteriores.</li>
 * </ul>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, EmailVerificationServiceRateLimitIT.TestBeans.class },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@DirtiesContext
class EmailVerificationServiceRateLimitIT extends AbstractSpringPostgresIT {

    /** Clock mutável compartilhado entre beans Spring e o teste. */
    static final AtomicReference<Instant> NOW =
            new AtomicReference<>(Instant.parse("2026-05-19T12:00:00Z"));

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }

        @Bean
        @Primary
        Clock movableClock() {
            return new Clock() {
                @Override
                public ZoneOffset getZone() {
                    return ZoneOffset.UTC;
                }
                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return this;
                }
                @Override
                public Instant instant() {
                    return NOW.get();
                }
            };
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

    @Autowired
    PlatformTransactionManager txManager;

    TransactionTemplate tx;

    @BeforeEach
    void resetClockAndMailer() {
        NOW.set(Instant.parse("2026-05-19T12:00:00Z"));
        mailer.clear();
        tx = new TransactionTemplate(txManager);
    }

    private Account persistedAccount(String email) {
        Account a = AccountTestFactory.newAdolescent(email);
        tx.executeWithoutResult(status -> em.persist(a));
        return a;
    }

    @Test
    void resend_withinCooldown_throwsRateLimited() {
        Account a = persistedAccount("rl1@example.com");
        service.resend(a);
        // 30s depois — dentro do cooldown de 60s
        NOW.set(NOW.get().plus(30, ChronoUnit.SECONDS));

        assertThatThrownBy(() -> service.resend(a))
                .isInstanceOf(EmailResendRateLimitedException.class)
                .satisfies(ex -> {
                    EmailResendRateLimitedException e = (EmailResendRateLimitedException) ex;
                    assertThat(e.getRetryAfterSeconds())
                            .as("retryAfter deve estar entre 1 e 60s")
                            .isBetween(1L, 60L);
                });
    }

    @Test
    void resend_afterCooldown_succeedsAndInvalidatesPreviousActiveToken() {
        Account a = persistedAccount("rl2@example.com");
        service.resend(a);
        // captura o token original ativo
        var firstActive = tokenRepository.findByAccountIdAndUsedAtIsNull(a.getId());
        assertThat(firstActive).hasSize(1);
        var originalToken = firstActive.get(0).getToken();

        // avança 61s — fora do cooldown
        NOW.set(NOW.get().plus(61, ChronoUnit.SECONDS));

        service.resend(a);

        var newActive = tokenRepository.findByAccountIdAndUsedAtIsNull(a.getId());
        assertThat(newActive)
                .as("após reenvio só o novo token deve ficar ativo")
                .hasSize(1);
        assertThat(newActive.get(0).getToken())
                .as("o novo token tem valor diferente do anterior")
                .isNotEqualTo(originalToken);

        // o token original foi marcado como usado
        var original = tokenRepository.findByToken(originalToken).orElseThrow();
        assertThat(original.getUsedAt()).isNotNull();
    }

    @Test
    void resend_exceedsHourlyLimit_throwsRateLimited() {
        Account a = persistedAccount("rl3@example.com");
        // 5 reenvios, cada um 61s após o anterior → todos dentro de 1h, mas
        // respeitando cooldown
        for (int i = 0; i < 5; i++) {
            service.resend(a);
            NOW.set(NOW.get().plus(61, ChronoUnit.SECONDS));
        }

        // 6º deve falhar — atingiu RESEND_MAX_PER_HOUR
        assertThatThrownBy(() -> service.resend(a))
                .isInstanceOf(EmailResendRateLimitedException.class);
    }
}
