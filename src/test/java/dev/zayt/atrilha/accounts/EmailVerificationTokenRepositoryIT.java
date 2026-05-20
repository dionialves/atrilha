package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.AtrilhaApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração do repositório de tokens de verificação de e-mail (US-006).
 *
 * <p>Valida que:
 * <ul>
 *   <li>Round-trip da entidade preserva todos os campos.</li>
 *   <li>{@code findByToken} resolve tokens existentes e retorna vazio para
 *       desconhecidos.</li>
 *   <li>{@code findByAccountIdAndUsedAtIsNull} traz apenas tokens ativos.</li>
 *   <li>{@code countByAccountIdAndCreatedAtAfter} suporta rate-limit por hora.</li>
 *   <li>{@code findFirstByAccountIdOrderByCreatedAtDesc} retorna o último
 *       token emitido (suporta cálculo de cooldown).</li>
 * </ul>
 * </p>
 */
@Testcontainers
@SpringBootTest(classes = AtrilhaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=false"
        })
@ActiveProfiles("test")
@DirtiesContext
@Transactional
class EmailVerificationTokenRepositoryIT {

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

    @Autowired
    EmailVerificationTokenRepository tokenRepository;

    @Autowired
    EntityManager em;

    private Account persistedAccount(String email) {
        Account account = AccountTestFactory.newAdolescent(email);
        em.persist(account);
        em.flush();
        return account;
    }

    private EmailVerificationToken newToken(UUID accountId, Instant expiresAt) {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setToken(UUID.randomUUID());
        t.setExpiresAt(expiresAt);
        t.setCreatedAt(Instant.now());
        return t;
    }

    @Test
    void persistsAndFindsByToken() {
        Account account = persistedAccount("repo1@example.com");
        EmailVerificationToken token = newToken(account.getId(),
                Instant.now().plus(24, ChronoUnit.HOURS));
        tokenRepository.saveAndFlush(token);
        em.clear();

        Optional<EmailVerificationToken> found = tokenRepository.findByToken(token.getToken());
        assertThat(found).isPresent();
        assertThat(found.get().getAccountId()).isEqualTo(account.getId());
        assertThat(found.get().getUsedAt()).isNull();
    }

    @Test
    void findByTokenReturnsEmptyForUnknown() {
        Optional<EmailVerificationToken> found = tokenRepository.findByToken(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void findByAccountIdAndUsedAtIsNullReturnsOnlyActiveTokens() {
        Account account = persistedAccount("repo2@example.com");
        Instant future = Instant.now().plus(24, ChronoUnit.HOURS);

        EmailVerificationToken active1 = newToken(account.getId(), future);
        EmailVerificationToken active2 = newToken(account.getId(), future);
        EmailVerificationToken usedAlready = newToken(account.getId(), future);
        usedAlready.setUsedAt(Instant.now());

        tokenRepository.saveAllAndFlush(List.of(active1, active2, usedAlready));
        em.clear();

        List<EmailVerificationToken> active =
                tokenRepository.findByAccountIdAndUsedAtIsNull(account.getId());
        assertThat(active).hasSize(2);
        assertThat(active).allMatch(t -> t.getUsedAt() == null);
    }

    @Test
    void countByAccountIdAndCreatedAtAfter_supportsHourlyRateLimit() {
        Account account = persistedAccount("repo3@example.com");
        Instant now = Instant.now();
        Instant future = now.plus(24, ChronoUnit.HOURS);

        EmailVerificationToken old = newToken(account.getId(), future);
        old.setCreatedAt(now.minus(2, ChronoUnit.HOURS));
        EmailVerificationToken recent1 = newToken(account.getId(), future);
        recent1.setCreatedAt(now.minus(30, ChronoUnit.MINUTES));
        EmailVerificationToken recent2 = newToken(account.getId(), future);
        recent2.setCreatedAt(now.minus(5, ChronoUnit.MINUTES));

        tokenRepository.saveAllAndFlush(List.of(old, recent1, recent2));
        em.clear();

        long count = tokenRepository.countByAccountIdAndCreatedAtAfter(
                account.getId(), now.minus(1, ChronoUnit.HOURS));
        assertThat(count)
                .as("apenas os criados na última hora")
                .isEqualTo(2);
    }

    @Test
    void findFirstByAccountIdOrderByCreatedAtDescReturnsMostRecent() {
        Account account = persistedAccount("repo4@example.com");
        Instant now = Instant.now();
        Instant future = now.plus(24, ChronoUnit.HOURS);

        EmailVerificationToken older = newToken(account.getId(), future);
        older.setCreatedAt(now.minus(2, ChronoUnit.MINUTES));
        EmailVerificationToken newer = newToken(account.getId(), future);
        newer.setCreatedAt(now.minus(10, ChronoUnit.SECONDS));

        tokenRepository.saveAllAndFlush(List.of(older, newer));
        em.clear();

        Optional<EmailVerificationToken> last =
                tokenRepository.findFirstByAccountIdOrderByCreatedAtDesc(account.getId());
        assertThat(last).isPresent();
        assertThat(last.get().getId()).isEqualTo(newer.getId());
    }
}
