package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import dev.zayt.atrilha.accounts.repository.AdolescentProfileRepository;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import dev.zayt.atrilha.auth.domain.AuthenticatedPrincipal;
import dev.zayt.atrilha.auth.login.AtrilhaUserDetails;
import dev.zayt.atrilha.auth.login.LoginAccountQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link PostLoginRedirectController} — testam a lógica de
 * displayName sem carregar o contexto Spring (compatible com Spring Boot 4.x
 * que removeu @WebMvcTest e @MockBean).
 */
class PostLoginRedirectControllerTest {

    private PostLoginRedirectController controller;
    private AdolescentProfileRepository profileRepo;

    @BeforeEach
    void setUp() {
        profileRepo = mock(AdolescentProfileRepository.class);
        controller = new PostLoginRedirectController(profileRepo);
    }

    // ---------- Teste 1: AuthenticatedAccount (fluxo de cadastro) ----------

    @Test
    void trilha_comAuthenticatedAccount_retorna200ComDisplayName() {
        UUID accountId = UUID.randomUUID();
        AuthenticatedAccount principal = new AuthenticatedAccount(accountId, AccountRole.TEEN);
        AdolescentProfile profile = new AdolescentProfile();
        profile.setAccountId(accountId);
        profile.setNickname("HeroiSabbath");

        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.of(profile));

        var auth = new UsernamePasswordAuthenticationToken(principal, null);
        Model model = new org.springframework.ui.ConcurrentModel();

        String view = controller.trilha(auth, model);

        assertThat(view).isEqualTo("trilha/placeholder");
        assertThat(model.getAttribute("displayName")).isEqualTo("HeroiSabbath");
    }

    // ---------- Teste 2: AtrilhaUserDetails (form login — compatibilidade reversa) ----------

    @Test
    void trilha_comAtrilhaUserDetails_retorna200ComDisplayName() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "teen@atrilha.test", "dummy", AccountRole.TEEN, false, "TeenUser");
        AtrilhaUserDetails userDetails = new AtrilhaUserDetails(account);

        var auth = new UsernamePasswordAuthenticationToken(userDetails, null);
        Model model = new org.springframework.ui.ConcurrentModel();

        String view = controller.trilha(auth, model);

        assertThat(view).isEqualTo("trilha/placeholder");
        assertThat(model.getAttribute("displayName")).isEqualTo("TeenUser");
    }

    // ---------- Teste 3: sem autenticação (null) — fallback defensivo ----------

    @Test
    void trilha_semAutenticacao_retornaFallbackAmigo() {
        Model model = new org.springframework.ui.ConcurrentModel();

        String view = controller.trilha(null, model);

        assertThat(view).isEqualTo("trilha/placeholder");
        assertThat(model.getAttribute("displayName")).isEqualTo("Amigo");
    }

    // ---------- Teste 4: AuthenticatedAccount sem perfil — fallback UUID substring ----------

    @Test
    void trilha_comAuthenticatedAccountSemPerfil_retornaSubstringDoUUID() {
        UUID accountId = UUID.randomUUID();
        AuthenticatedAccount principal = new AuthenticatedAccount(accountId, AccountRole.TEEN);

        when(profileRepo.findByAccountId(accountId)).thenReturn(Optional.empty());

        var auth = new UsernamePasswordAuthenticationToken(principal, null);
        Model model = new org.springframework.ui.ConcurrentModel();

        String view = controller.trilha(auth, model);

        assertThat(view).isEqualTo("trilha/placeholder");
        String expected = accountId.toString().substring(0, 8);
        assertThat(model.getAttribute("displayName")).isEqualTo(expected);
    }

    // ---------- Teste 5: principal desconhecido — fallback "Amigo" ----------

    @Test
    void trilha_comPrincipalDesconhecido_retornaFallbackAmigo() {
        var auth = new UsernamePasswordAuthenticationToken("unknown", null);
        Model model = new org.springframework.ui.ConcurrentModel();

        String view = controller.trilha(auth, model);

        assertThat(view).isEqualTo("trilha/placeholder");
        assertThat(model.getAttribute("displayName")).isEqualTo("Amigo");
    }
}
