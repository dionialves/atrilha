package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.AccountRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes para {@link InMemoryLoginAccountQuery} — US-007.03.
 *
 * <p>Valida carregamento das 3 seeds, lookup case-insensitive e retorno empty
 * para email desconhecido.</p>
 */
@SpringBootTest(classes = dev.zayt.atrilha.AtrilhaApplication.class, properties = {
        "spring.profiles.active=test",
        // Desliga modo web — o bean sobe no perfil test sem precisar de servidor.
        "spring.main.web-application-type=none"
})
class InMemoryLoginAccountQueryTest {

    @Autowired
    private LoginAccountQuery query;

    // ---- Seed loading ----

    @Test
    void findForLogin_teen_seed_loaded() {
        Optional<dev.zayt.atrilha.auth.login.LoginAccountQuery.LoginAccount> result =
                query.findForLogin("teen@atrilha.test");
        assertThat(result).isPresent();

        var account = result.get();
        assertThat(account.email()).isEqualTo("teen@atrilha.test");
        assertThat(account.role()).isEqualTo(AccountRole.TEEN);
        assertThat(account.hasGuardianLink()).isFalse();
        assertThat(account.displayName()).isEqualTo("teen");
        assertIsBcrypt(account.passwordHashBcrypt());
    }

    @Test
    void findForLogin_guardianLinked_seed_loaded() {
        Optional<dev.zayt.atrilha.auth.login.LoginAccountQuery.LoginAccount> result =
                query.findForLogin("guardian@atrilha.test");
        assertThat(result).isPresent();

        var account = result.get();
        assertThat(account.email()).isEqualTo("guardian@atrilha.test");
        assertThat(account.role()).isEqualTo(AccountRole.GUARDIAN);
        assertThat(account.hasGuardianLink()).isTrue();
        assertThat(account.displayName()).isEqualTo("guardian");
        assertIsBcrypt(account.passwordHashBcrypt());
    }

    @Test
    void findForLogin_guardianUnlinked_seed_loaded() {
        Optional<dev.zayt.atrilha.auth.login.LoginAccountQuery.LoginAccount> result =
                query.findForLogin("guardian-new@atrilha.test");
        assertThat(result).isPresent();

        var account = result.get();
        assertThat(account.email()).isEqualTo("guardian-new@atrilha.test");
        assertThat(account.role()).isEqualTo(AccountRole.GUARDIAN);
        assertThat(account.hasGuardianLink()).isFalse();
        assertThat(account.displayName()).isEqualTo("guardian-new");
        assertIsBcrypt(account.passwordHashBcrypt());
    }

    // ---- Case-insensitive lookup ----

    @Test
    void findForLogin_uppercase_email_resolves() {
        Optional<dev.zayt.atrilha.auth.login.LoginAccountQuery.LoginAccount> result =
                query.findForLogin("TEEN@ATRILHA.TEST");
        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("teen@atrilha.test");
    }

    @Test
    void findForLogin_mixedCase_email_resolves() {
        Optional<dev.zayt.atrilha.auth.login.LoginAccountQuery.LoginAccount> result =
                query.findForLogin("Guardian@Atrilha.Test");
        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("guardian@atrilha.test");
    }

    // ---- Unknown email ----

    @Test
    void findForLogin_unknown_email_returnsEmpty() {
        Optional<dev.zayt.atrilha.auth.login.LoginAccountQuery.LoginAccount> result =
                query.findForLogin("nao-existe@x");
        assertThat(result).isEmpty();
    }

    @Test
    void findForLogin_emptyString_returnsEmpty() {
        Optional<dev.zayt.atrilha.auth.login.LoginAccountQuery.LoginAccount> result =
                query.findForLogin("");
        assertThat(result).isEmpty();
    }

    // ---- Helpers ----

    private void assertIsBcrypt(String hash) {
        assertThat(hash)
                .isNotNull()
                .startsWith("$2a$")
                .hasSizeGreaterThan(50);
    }
}
