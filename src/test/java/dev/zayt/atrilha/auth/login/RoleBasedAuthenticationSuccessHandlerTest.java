package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.AccountRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleBasedAuthenticationSuccessHandlerTest {

    private LoginAttemptService loginAttemptService;
    private RoleBasedAuthenticationSuccessHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        loginAttemptService = mock(LoginAttemptService.class);
        handler = new RoleBasedAuthenticationSuccessHandler(loginAttemptService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    // ---- resolveTrilhaParaTeen (form login) ----

    @Test
    @DisplayName("resolveTrilhaParaTeen")
    void resolveTrilhaParaTeen() {
        LoginAccountQuery.LoginAccount teenAccount = new LoginAccountQuery.LoginAccount(
                "teen@test.com", "$2a$10$hash", AccountRole.TEEN, false, "Juca");
        AtrilhaUserDetails userDetails = new AtrilhaUserDetails(teenAccount);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.TRILHA, destination);
    }

    // ---- resolvePainelParaGuardianVinculado (form login) ----

    @Test
    @DisplayName("resolvePainelParaGuardianVinculado")
    void resolvePainelParaGuardianVinculado() {
        LoginAccountQuery.LoginAccount guardianAccount = new LoginAccountQuery.LoginAccount(
                "guardian@test.com", "$2a$10$hash", AccountRole.GUARDIAN, true, "Maria");
        AtrilhaUserDetails userDetails = new AtrilhaUserDetails(guardianAccount);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.PAINEL, destination);
    }

    // ---- resolveVincularParaGuardianSemVinculo (form login) ----

    @Test
    @DisplayName("resolveVincularParaGuardianSemVinculo")
    void resolveVincularParaGuardianSemVinculo() {
        LoginAccountQuery.LoginAccount guardianAccount = new LoginAccountQuery.LoginAccount(
                "guardian2@test.com", "$2a$10$hash", AccountRole.GUARDIAN, false, "Joao");
        AtrilhaUserDetails userDetails = new AtrilhaUserDetails(guardianAccount);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.VINCULAR, destination);
    }

    // ---- OAuth principal (AtrilhaOAuth2User) com match TEEN → /trilha ----

    @Test
    @DisplayName("oauthPrincipalComMatchTeenRetornaTrilha")
    void oauthPrincipalComMatchTeenRetornaTrilha() {
        LoginAccountQuery.LoginAccount teenAccount = new LoginAccountQuery.LoginAccount(
                "teen-oauth@test.com", null, AccountRole.TEEN, false, "Juca");
        AtrilhaOAuth2User oauthPrincipal = new AtrilhaOAuth2User(teenAccount,
                java.util.Map.of("email", "teen-oauth@test.com"));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauthPrincipal);

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.TRILHA, destination);
    }

    // ---- OAuth principal (AtrilhaOAuth2User) com match GUARDIAN vinculado → /painel ----

    @Test
    @DisplayName("oauthPrincipalComMatchGuardianVinculadoRetornaPainel")
    void oauthPrincipalComMatchGuardianVinculadoRetornaPainel() {
        LoginAccountQuery.LoginAccount guardianAccount = new LoginAccountQuery.LoginAccount(
                "guardian-oauth@test.com", null, AccountRole.GUARDIAN, true, "Maria");
        AtrilhaOAuth2User oauthPrincipal = new AtrilhaOAuth2User(guardianAccount,
                java.util.Map.of("email", "guardian-oauth@test.com"));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauthPrincipal);

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.PAINEL, destination);
    }

    // ---- Principal desconhecido → /login?error ----

    @Test
    @DisplayName("principalDesconhecidoRetornaError")
    void principalDesconhecidoRetornaError() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new Object());

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.ERROR, destination);
    }

    // ---- onAuthenticationSuccess redireciona para destino correto (TEEN) ----

    @Test
    @DisplayName("onAuthenticationSuccessTeenRedirecionaParaTrilha")
    void onAuthenticationSuccessTeenRedirecionaParaTrilha() throws IOException {
        LoginAccountQuery.LoginAccount teenAccount = new LoginAccountQuery.LoginAccount(
                "teen@test.com", "$2a$10$hash", AccountRole.TEEN, false, "Juca");
        AtrilhaUserDetails userDetails = new AtrilhaUserDetails(teenAccount);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/trilha");
    }

    // ---- onAuthenticationSuccess redireciona para destino correto (GUARDIAN vinculado) ----

    @Test
    @DisplayName("onAuthenticationSuccessGuardianVinculadoRedirecionaParaPainel")
    void onAuthenticationSuccessGuardianVinculadoRedirecionaParaPainel() throws IOException {
        LoginAccountQuery.LoginAccount guardianAccount = new LoginAccountQuery.LoginAccount(
                "guardian@test.com", "$2a$10$hash", AccountRole.GUARDIAN, true, "Maria");
        AtrilhaUserDetails userDetails = new AtrilhaUserDetails(guardianAccount);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(request.getRemoteAddr()).thenReturn("10.0.0.2");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/painel");
    }

    // ---- onAuthenticationSuccess redireciona para destino correto (GUARDIAN sem vínculo) ----

    @Test
    @DisplayName("onAuthenticationSuccessGuardianSemVinculoRedirecionaParaVincular")
    void onAuthenticationSuccessGuardianSemVinculoRedirecionaParaVincular() throws IOException {
        LoginAccountQuery.LoginAccount guardianAccount = new LoginAccountQuery.LoginAccount(
                "guardian2@test.com", "$2a$10$hash", AccountRole.GUARDIAN, false, "Joao");
        AtrilhaUserDetails userDetails = new AtrilhaUserDetails(guardianAccount);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(request.getRemoteAddr()).thenReturn("10.0.0.3");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/vincular");
    }

    // ---- resolveForRole direto (sem Authentication) ----

    @Test
    @DisplayName("resolveForRoleTeenRetornaTrilha")
    void resolveForRoleTeenRetornaTrilha() {
        assertEquals(PostLoginDestination.TRILHA, handler.resolveForRole(AccountRole.TEEN, false));
        assertEquals(PostLoginDestination.TRILHA, handler.resolveForRole(AccountRole.TEEN, true));
    }

    @Test
    @DisplayName("resolveForRoleGuardianVinculadoRetornaPainel")
    void resolveForRoleGuardianVinculadoRetornaPainel() {
        assertEquals(PostLoginDestination.PAINEL, handler.resolveForRole(AccountRole.GUARDIAN, true));
    }

    @Test
    @DisplayName("resolveForRoleGuardianSemVinculoRetornaVincular")
    void resolveForRoleGuardianSemVinculoRetornaVincular() {
        assertEquals(PostLoginDestination.VINCULAR, handler.resolveForRole(AccountRole.GUARDIAN, false));
    }

    // ---- extractUsername para form login ----

    @Test
    @DisplayName("extractUsernameParaFormLogin")
    void extractUsernameParaFormLogin() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "user@test.com", "$2a$10$hash", AccountRole.TEEN, false, "User");
        AtrilhaUserDetails userDetails = new AtrilhaUserDetails(account);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        assertEquals("user@test.com", handler.extractUsername(authentication));
    }

    // ---- extractUsername para OAuth (AtrilhaOAuth2User) ----

    @Test
    @DisplayName("extractUsernameParaOAuth")
    void extractUsernameParaOAuth() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "OAuth@TEST.COM", null, AccountRole.TEEN, false, "OUser");
        AtrilhaOAuth2User oauthPrincipal = new AtrilhaOAuth2User(account,
                java.util.Map.of("email", "OAuth@TEST.COM"));

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauthPrincipal);

        assertEquals("OAuth@TEST.COM", handler.extractUsername(authentication));
    }

    // ---- extractUsername para principal desconhecido ----

    @Test
    @DisplayName("extractUsernameParaPrincipalDesconhecidoRetornaNull")
    void extractUsernameParaPrincipalDesconhecidoRetornaNull() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new Object());

        assertNull(handler.extractUsername(authentication));
    }
}
