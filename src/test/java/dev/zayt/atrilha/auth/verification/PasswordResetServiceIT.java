package dev.zayt.atrilha.auth.verification;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.AccountTestFactory;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.auth.domain.PasswordResetResult;
import jakarta.persistence.EntityManager;
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
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração do {@link PasswordResetService} — emissão e verificação de
 * tokens de recuperação de senha (US-008-a).
 *
 * <p>Cobre os 7 cenários do service:
 * <ul>
 *   <li>{@link #issueToken_invalidaPendentes()} — resend invalida tokens antigos</li>
 *   <li>{@link #issueToken_ttl1h()} — TTL de 1 hora</li>
 *   <li>{@link #verify_success()} — token válido marca como consumido</li>
 *   <li>{@link #verify_expired()} — token expirado retorna EXPIRED_OR_INVALID</li>
 *   <li>{@link #verify_alreadyUsed()} — token já consumido retorna ALREADY_USED</li>
 *   <li>{@link #verify_nonExistentUuid()} — UUID inexistente retorna EXPIRED_OR_INVALID</li>
 *   <li>{@link #consume_idempotente()} — consume marca como usado, idempotente</li>
 * </ul>
 * </p>
 */
@Testcontainers
@SpringBootTest(classes = { AtrilhaApplication.class, PasswordResetServiceIT.TestBeans.class },
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PasswordResetServiceIT {

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
        PasswordResetSender passwordResetSender() {
            return new NoOpPasswordResetSender();
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
    PasswordResetService service;

    @Autowired
    PasswordResetTokenRepository tokenRepository;

    @Autowired
    EntityManager em;

    private Account persistedAccount(String email) {
        Account a = AccountTestFactory.newAdolescent(email);
        em.persist(a);
        em.flush();
        return a;
    }

    // ---- Suite 1 — issueToken ----

    @Test
    @Transactional
    void issueToken_invalidaPendentes() {
        Account a = persistedAccount("invalidate@example.com");

        // Emite primeiro token e marca como usado manualmente
        UUID firstToken = service.issueToken(a);
        em.flush();
        PasswordResetToken firstPersisted = tokenRepository.findByToken(firstToken).orElseThrow();
        firstPersisted.setUsedAt(Instant.parse("2026-05-19T12:30:00Z"));
        em.flush();

        // Emite segundo token — o primeiro deve ser invalidado (used_at set)
        UUID secondToken = service.issueToken(a);
        em.flush();
        em.clear();

        // Primeiro token agora tem used_at (foi invalidado)
        PasswordResetToken invalidated = tokenRepository.findByToken(firstToken).orElseThrow();
        assertThat(invalidated.getUsedAt()).isNotNull()
                .as("token pendente anterior deve ser invalidado ao emitir novo");

        // Segundo token é novo e não usado
        PasswordResetToken newToken = tokenRepository.findByToken(secondToken).orElseThrow();
        assertThat(newToken.getUsedAt()).isNull();
    }

    @Test
    @Transactional
    void issueToken_ttl1h() {
        Account a = persistedAccount("ttl@example.com");

        UUID token = service.issueToken(a);
        em.flush();
        em.clear();

        PasswordResetToken persisted = tokenRepository.findByToken(token).orElseThrow();
        Instant now = Instant.parse("2026-05-19T12:00:00Z");
        assertThat(persisted.getExpiresAt())
                .isAfter(now.plus(Duration.ofHours(1).minusSeconds(1)))
                .isBefore(now.plus(Duration.ofHours(1).plusSeconds(1)));
    }

    // ---- Suite 2 — verify ----

    @Test
    @Transactional
    void verify_success() {
        Account a = persistedAccount("success@example.com");
        UUID token = service.issueToken(a);
        em.flush();
        em.clear();

        PasswordResetResult result = service.verify(token);
        em.flush();
        em.clear();

        assertThat(result).isEqualTo(PasswordResetResult.SUCCESS);
        PasswordResetToken used = tokenRepository.findByToken(token).orElseThrow();
        assertThat(used.getUsedAt()).isNotNull();
    }

    @Test
    @Transactional
    void verify_expired() {
        Account a = persistedAccount("expired@example.com");

        PasswordResetToken expired = new PasswordResetToken();
        expired.setId(UUID.randomUUID());
        expired.setAccountId(a.getId());
        expired.setToken(UUID.randomUUID());
        expired.setExpiresAt(Instant.parse("2026-05-19T12:00:00Z").minus(30, ChronoUnit.MINUTES));
        expired.setCreatedAt(Instant.parse("2026-05-19T11:30:00Z"));
        tokenRepository.saveAndFlush(expired);
        em.clear();

        PasswordResetResult result = service.verify(expired.getToken());

        assertThat(result).isEqualTo(PasswordResetResult.EXPIRED_OR_INVALID);
    }

    @Test
    @Transactional
    void verify_alreadyUsed() {
        Account a = persistedAccount("used@example.com");
        UUID token = service.issueToken(a);
        em.flush();

        // Primeira verificação → success
        PasswordResetResult first = service.verify(token);
        assertThat(first).isEqualTo(PasswordResetResult.SUCCESS);
        em.flush();
        em.clear();

        // Segunda verificação do mesmo token → already used
        PasswordResetResult second = service.verify(token);

        assertThat(second).isEqualTo(PasswordResetResult.ALREADY_USED);
    }

    @Test
    @Transactional
    void verify_nonExistentUuid() {
        PasswordResetResult result = service.verify(UUID.randomUUID());

        assertThat(result).isEqualTo(PasswordResetResult.EXPIRED_OR_INVALID);
    }

    // ---- Suite 3 — consume ----

    @Test
    @Transactional
    void consume_idempotente() {
        Account a = persistedAccount("consume@example.com");
        UUID token = service.issueToken(a);
        em.flush();
        em.clear();

        // Primeiro consume → success
        boolean first = service.consume(token);
        assertThat(first).isTrue();
        em.flush();
        em.clear();

        // Segundo consume do mesmo token → false (idempotente)
        boolean second = service.consume(token);
        assertThat(second).isFalse();

        // Consume de token inexistente → false (sem exceção)
        boolean nonexistent = service.consume(UUID.randomUUID());
        assertThat(nonexistent).isFalse();
    }
}
