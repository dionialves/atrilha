package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.repository.AccountReader;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.auth.verification.RequiresVerifiedEmail;
import dev.zayt.atrilha.auth.verification.RequiresVerifiedEmailInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link RequiresVerifiedEmailInterceptor} — ponto de
 * extensão declarado nesta US para uso futuro por US-012/US-014 (vinculação).
 *
 * <p>Cobre as combinações:
 * <ul>
 *   <li>Endpoint anotado + usuário não verificado → bloqueio (redirect GET / 403 POST).</li>
 *   <li>Endpoint anotado + usuário verificado → passa.</li>
 *   <li>Endpoint não anotado + usuário não verificado → passa.</li>
 *   <li>Endpoint anotado + anonymous → passa (auth não é responsabilidade do interceptor;
 *       Spring Security já decide antes).</li>
 * </ul>
 * </p>
 */
class RequiresVerifiedEmailInterceptorTest {

    private AccountReader accountReader;
    private RequiresVerifiedEmailInterceptor interceptor;

    private Method annotatedMethod;
    private Method unannotatedMethod;

    @BeforeEach
    void setUp() throws Exception {
        accountReader = mock(AccountReader.class);
        interceptor = new RequiresVerifiedEmailInterceptor(accountReader);

        annotatedMethod = SampleHandler.class.getDeclaredMethod("annotated");
        unannotatedMethod = SampleHandler.class.getDeclaredMethod("unannotated");
    }

    private HandlerMethod handler(Method m) {
        return new HandlerMethod(new SampleHandler(), m);
    }

    private void authenticateAs(UUID accountId) {
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedAccount(accountId, AccountRole.TEEN),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TEEN")));
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAnnotatedEndpoint_userNotVerified_redirectsToPending() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account a = new Account();
        a.setId(accountId);
        a.setEmailVerifiedAt(null);
        when(accountReader.findById(accountId)).thenReturn(Optional.of(a));
        authenticateAs(accountId);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/sensivel");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle((HttpServletRequest) req, (HttpServletResponse) resp, handler(annotatedMethod));

        assertThat(proceed).isFalse();
        assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
        assertThat(resp.getRedirectedUrl()).isEqualTo("/verificar-email");
    }

    @Test
    void getAnnotatedEndpoint_userVerified_passesThrough() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account a = new Account();
        a.setId(accountId);
        a.setEmailVerifiedAt(OffsetDateTime.now());
        when(accountReader.findById(accountId)).thenReturn(Optional.of(a));
        authenticateAs(accountId);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/sensivel");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle((HttpServletRequest) req, (HttpServletResponse) resp, handler(annotatedMethod));

        assertThat(proceed).isTrue();
        assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void postAnnotatedEndpoint_userNotVerified_returns403() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account a = new Account();
        a.setId(accountId);
        a.setEmailVerifiedAt(null);
        when(accountReader.findById(accountId)).thenReturn(Optional.of(a));
        authenticateAs(accountId);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/sensivel");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle((HttpServletRequest) req, (HttpServletResponse) resp, handler(annotatedMethod));

        assertThat(proceed).isFalse();
        assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void nonAnnotatedEndpoint_userNotVerified_passesThrough() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account a = new Account();
        a.setId(accountId);
        a.setEmailVerifiedAt(null);
        when(accountReader.findById(accountId)).thenReturn(Optional.of(a));
        authenticateAs(accountId);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/aberto");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle((HttpServletRequest) req, (HttpServletResponse) resp, handler(unannotatedMethod));

        assertThat(proceed).isTrue();
    }

    @Test
    void annotatedEndpoint_anonymous_passesThrough() throws Exception {
        // Anonymous: SecurityContext sem authentication. Interceptor deixa passar
        // — autorização (login) é responsabilidade do Spring Security antes.
        SecurityContextHolder.clearContext();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/sensivel");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle((HttpServletRequest) req, (HttpServletResponse) resp, handler(annotatedMethod));

        assertThat(proceed).isTrue();
    }

    // Handler stub para extrair Method com/sem anotação.
    static class SampleHandler {
        @RequiresVerifiedEmail
        public void annotated() {
            // no-op
        }

        public void unannotated() {
            // no-op
        }
    }
}
