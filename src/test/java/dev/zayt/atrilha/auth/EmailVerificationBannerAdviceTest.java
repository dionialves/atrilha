package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.repository.AccountReader;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.auth.web.EmailVerificationBannerAdvice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link EmailVerificationBannerAdvice}.
 *
 * <p>O advice expõe um {@code @ModelAttribute("unverifiedEmail")} de tipo
 * {@code boolean} consumido pelo {@code base.html} para renderizar o banner
 * persistente (US-006 §7).</p>
 */
class EmailVerificationBannerAdviceTest {

    private final AccountReader accountReader = mock(AccountReader.class);
    private final EmailVerificationBannerAdvice advice = new EmailVerificationBannerAdvice(accountReader);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UUID id) {
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedAccount(id, AccountRole.TEEN), null,
                List.of(new SimpleGrantedAuthority("ROLE_TEEN")));
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void unverifiedAccount_setsFlagTrue() {
        UUID id = UUID.randomUUID();
        Account a = new Account();
        a.setId(id);
        a.setEmailVerifiedAt(null);
        when(accountReader.findById(id)).thenReturn(Optional.of(a));
        authenticateAs(id);

        assertThat(advice.unverifiedEmail()).isTrue();
    }

    @Test
    void verifiedAccount_setsFlagFalse() {
        UUID id = UUID.randomUUID();
        Account a = new Account();
        a.setId(id);
        a.setEmailVerifiedAt(OffsetDateTime.now());
        when(accountReader.findById(id)).thenReturn(Optional.of(a));
        authenticateAs(id);

        assertThat(advice.unverifiedEmail()).isFalse();
    }

    @Test
    void anonymousUser_setsFlagFalse() {
        // Sem authentication no contexto.
        assertThat(advice.unverifiedEmail()).isFalse();
    }

    @Test
    void unknownAccount_setsFlagFalse() {
        // Authentication válida mas conta deletada do banco (caso raro).
        UUID id = UUID.randomUUID();
        when(accountReader.findById(id)).thenReturn(Optional.empty());
        authenticateAs(id);

        assertThat(advice.unverifiedEmail()).isFalse();
    }

    // ---- showEmailVerificationBanner ----

    private MockHttpServletRequest reqAt(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", uri);
        r.setRequestURI(uri);
        return r;
    }

    @Test
    void showBanner_onArbitraryRoute_whenUnverified_returnsTrue() {
        UUID id = UUID.randomUUID();
        Account a = new Account();
        a.setId(id);
        a.setEmailVerifiedAt(null);
        when(accountReader.findById(id)).thenReturn(Optional.of(a));
        authenticateAs(id);

        assertThat(advice.showEmailVerificationBanner(reqAt("/"))).isTrue();
        assertThat(advice.showEmailVerificationBanner(reqAt("/cadastro/adolescente"))).isTrue();
    }

    @Test
    void showBanner_onVerifyEmailPages_returnsFalse() {
        UUID id = UUID.randomUUID();
        Account a = new Account();
        a.setId(id);
        a.setEmailVerifiedAt(null);
        when(accountReader.findById(id)).thenReturn(Optional.of(a));
        authenticateAs(id);

        assertThat(advice.showEmailVerificationBanner(reqAt("/verificar-email"))).isFalse();
        assertThat(advice.showEmailVerificationBanner(reqAt("/verificar-email/reenviar"))).isFalse();
        assertThat(advice.showEmailVerificationBanner(reqAt("/verify-email"))).isFalse();
        assertThat(advice.showEmailVerificationBanner(reqAt("/verify-email?token=abc"))).isFalse();
    }

    @Test
    void showBanner_whenVerified_returnsFalse() {
        UUID id = UUID.randomUUID();
        Account a = new Account();
        a.setId(id);
        a.setEmailVerifiedAt(java.time.OffsetDateTime.now());
        when(accountReader.findById(id)).thenReturn(Optional.of(a));
        authenticateAs(id);

        assertThat(advice.showEmailVerificationBanner(reqAt("/"))).isFalse();
    }

    @Test
    void showBanner_whenAnonymous_returnsFalse() {
        // sem auth
        assertThat(advice.showEmailVerificationBanner(reqAt("/"))).isFalse();
    }
}
