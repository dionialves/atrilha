package dev.zayt.atrilha.auth.login;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para {@link JpaLoginAccountQuery} focados em GUARDIAN
 * (US-003).
 *
 * <p>Valida que {@code resolveDisplayName()} retorna o full_name do
 * GuardianProfile quando a conta é GUARDIAN, e faz fallback para prefixo
 * do email quando não há perfil.</p>
 */
@Testcontainers
@SpringBootTest(classes = { JpaLoginAccountQueryTest.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "atrilha.auth.seed.enabled=false"
        })
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JpaLoginAccountQueryTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("atrilha")
                    .withUsername("atrilha")
                    .withPassword("atrilha");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private LoginAccountQuery loginAccountQuery;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---- resolveDisplayName() para GUARDIAN ----

    @Test
    @DisplayName("resolveDisplayNameUsesFullNameForGuardian")
    void resolveDisplayNameUsesFullNameForGuardian() {
        UUID accountId = UUID.randomUUID();
        String email = "carlos@teste.com";
        String hashed = passwordEncoder.encode("senha");

        insertAccount(accountId, "GUARDIAN", email, hashed);
        insertGuardianProfile(accountId, "Carlos Responsável");

        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin(email);

        assertThat(result).isPresent();
        assertThat(result.get().displayName()).isEqualTo("Carlos Responsável");
    }

    @Test
    @DisplayName("resolveDisplayNameFallsBackToEmailPrefixForGuardianWhenNoProfile")
    void resolveDisplayNameFallsBackToEmailPrefixForGuardianWhenNoProfile() {
        UUID accountId = UUID.randomUUID();
        String email = "guardian@teste.com";

        insertAccount(accountId, "GUARDIAN", email, passwordEncoder.encode("senha"));
        // Não cria GuardianProfile — cenário de borda.

        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin(email);

        assertThat(result).isPresent();
        assertThat(result.get().displayName()).isEqualTo("guardian");
    }

    // ---- Helpers ----

    private void insertAccount(UUID id, String type, String email, String passwordHash) {
        jdbcTemplate.update(
                """
                        INSERT INTO accounts (id, type, email, password_hash, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                id, type, email, passwordHash, OffsetDateTime.now());
    }

    private void insertGuardianProfile(UUID accountId, String fullName) {
        jdbcTemplate.update(
                """
                        INSERT INTO guardian_profiles (account_id, full_name)
                        VALUES (?, ?)
                        """,
                accountId, fullName);
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestBeans {
        // InMemoryLoginAccountQuery desligado por atrilha.auth.seed.enabled=false
    }
}
