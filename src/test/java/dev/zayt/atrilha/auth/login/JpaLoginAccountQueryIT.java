package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.testsupport.AbstractSpringPostgresIT;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Testes de integra&cced;&atilde;o para {@link JpaLoginAccountQuery} (FIX-013).
 *
 * <p>Valida que a implementa&ccedil;&atilde;o JPA do {@code LoginAccountQuery}
 * l&ecirc; contas reais da tabela {@code accounts}, respeitando soft-delete,
 * case-insensitive e mapeamento correto de papel/displayName.</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, JpaLoginAccountQueryIT.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration"
        })
@ActiveProfiles("test")
@TestPropertySource(properties = "atrilha.auth.seed.enabled=false")
class JpaLoginAccountQueryIT extends AbstractSpringPostgresIT {

    @Autowired
    private LoginAccountQuery loginAccountQuery;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---- Testes da ordem TDD (FIX-013) ----

    /**
     * Teste 1: findForLogin retorna conta adolescente persistida.
     */
    @Test
    @DisplayName("findForLoginRetornaContaAdolescentePersistida")
    void findForLogin_retornaContaAdolescentePersistida() {
        UUID accountId = UUID.randomUUID();
        String email = "julia1@exemplo.com";
        String hashed = passwordEncoder.encode("umaSenhaForte");

        insertAccount(accountId, "ADOLESCENT", email, hashed);
        insertAdolescentProfile(accountId, "Julia");

        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin(email);

        assertThat(result).isPresent();
        var la = result.get();
        assertThat(la.email()).isEqualTo(email);
        assertThat(la.passwordHashBcrypt()).isEqualTo(hashed);
        assertThat(la.role()).isEqualTo(dev.zayt.atrilha.accounts.domain.AccountRole.TEEN);
        assertThat(la.hasGuardianLink()).isFalse();
        assertThat(la.displayName()).isEqualTo("Julia");
    }

    /**
     * Teste 2: findForLogin case-insensitive.
     */
    @Test
    @DisplayName("findForLoginCaseInsensitive")
    void findForLogin_caseInsensitive() {
        UUID accountId = UUID.randomUUID();
        String email = "julia2@exemplo.com";

        insertAccount(accountId, "ADOLESCENT", email, passwordEncoder.encode("senha"));

        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin("JULIA2@Exemplo.COM");

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo(email);
    }

    /**
     * Teste 3: findForLogin ignora conta soft-deletada.
     */
    @Test
    @DisplayName("findForLoginIgnoraContaSoftDeletada")
    void findForLogin_ignoraContaSoftDeletada() {
        UUID accountId = UUID.randomUUID();
        String email = "deleted@exemplo.com";

        insertAccount(accountId, "ADOLESCENT", email, passwordEncoder.encode("senha"));
        jdbcTemplate.update(
                "UPDATE accounts SET deleted_at = ? WHERE id = ?",
                OffsetDateTime.now().minusDays(1), accountId);

        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin(email);

        assertThat(result).isEmpty();
    }

    /**
     * Teste 4: findForLogin retorna empty para email inexistente.
     */
    @Test
    @DisplayName("findForLoginRetornaEmptyParaEmailInexistente")
    void findForLogin_retornaEmptyParaEmailInexistente() {
        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin("nao-existe@x.com");

        assertThat(result).isEmpty();
    }

    /**
     * Teste 5: findForLogin guardian retorna hasGuardianLink false (Sprint 3).
     */
    @Test
    @DisplayName("findForLoginGuardianRetornaHasGuardianLinkFalse")
    void findForLogin_guardianRetornaHasGuardianLinkFalse() {
        UUID accountId = UUID.randomUUID();
        String email = "guardiao@exemplo.com";

        insertAccount(accountId, "GUARDIAN", email, passwordEncoder.encode("senha"));

        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin(email);

        assertThat(result).isPresent();
        var la = result.get();
        assertThat(la.role()).isEqualTo(dev.zayt.atrilha.accounts.domain.AccountRole.GUARDIAN);
        assertThat(la.hasGuardianLink()).isFalse();
    }

    /**
     * Teste 6: findForLogin adolescente sem profile usa prefixo email como displayName.
     */
    @Test
    @DisplayName("findForLoginAdolescenteSemProfileUsaPrefixoEmailComoDisplayName")
    void findForLogin_adolescenteSemProfile_usaPrefixoEmailComoDisplayName() {
        UUID accountId = UUID.randomUUID();
        String email = "semnick@exemplo.com";

        insertAccount(accountId, "ADOLESCENT", email, passwordEncoder.encode("senha"));
        // N&atilde;o cria adolescent_profile — cen&a;rio defensivo.

        Optional<LoginAccountQuery.LoginAccount> result = loginAccountQuery.findForLogin(email);

        assertThat(result).isPresent();
        var la = result.get();
        assertThat(la.displayName()).isEqualTo("semnick");
    }

    /**
     * Teste 7: findForLogin null/blank retorna empty.
     */
    @Test
    @DisplayName("findForLoginNullERetornaEmpty")
    void findForLogin_nullERetornaEmpty() {
        assertThat(loginAccountQuery.findForLogin(null)).isEmpty();
        assertThat(loginAccountQuery.findForLogin("")).isEmpty();
        assertThat(loginAccountQuery.findForLogin("   ")).isEmpty();
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

    private void insertAdolescentProfile(UUID accountId, String nickname) {
        jdbcTemplate.update(
                """
                        INSERT INTO adolescent_profiles (account_id, nickname, birth_date, timezone)
                        VALUES (?, ?, ?, ?)
                        """,
                accountId, nickname, java.time.LocalDate.of(2013, 6, 15), "America/Sao_Paulo");
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestBeans {
        // InMemoryLoginAccountQuery desligado por atrilha.auth.seed.enabled=false
    }
}
