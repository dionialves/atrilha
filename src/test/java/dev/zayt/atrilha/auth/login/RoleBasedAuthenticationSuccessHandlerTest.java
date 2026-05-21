package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.AccountRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleBasedAuthenticationSuccessHandlerTest {

    private LoginAccountQuery loginAccountQuery;
    private LoginAttemptService loginAttemptService;
    private RoleBasedAuthenticationSuccessHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        loginAccountQuery = mock(LoginAccountQuery.class);
        loginAttemptService = mock(LoginAttemptService.class);
        handler = new RoleBasedAuthenticationSuccessHandler(loginAccountQuery, loginAttemptService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    // ---- resolveTrilhaParaTeen ----

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

    // ---- resolvePainelParaGuardianVinculado ----

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

    // ---- resolveVincularParaGuardianSemVinculo ----

    @Test
    @DisplayName("resolveVincularParaGuardianSemVinculo")
    void resolveVincularParaGuardianSemVinculo() {
        LoginAccountQuery.LoginAccount guardianAccount = new LoginAccountQuery.LoginAccount(
                "guardian2@test.com", "$2a$10$hash", AccountRole.GUARDIAN, false, "João");
        AtrilhaUserDetails userDetails = new AtrilhaUserDetails(guardianAccount);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.VINCULAR, destination);
    }

    // ---- OAuth principal sem match → /login?error ----

    @Test
    @DisplayName("oauthPrincipalSemMatchRetornaError")
    void oauthPrincipalSemMatchRetornaError() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("email")).thenReturn("unknown@test.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        when(loginAccountQuery.findForLogin("unknown@test.com")).thenReturn(java.util.Optional.empty());

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.ERROR, destination);
    }

    // ---- OAuth principal com match TEEN → /trilha ----

    @Test
    @DisplayName("oauthPrincipalComMatchTeenRetornaTrilha")
    void oauthPrincipalComMatchTeenRetornaTrilha() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("email")).thenReturn("teen-oauth@test.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        LoginAccountQuery.LoginAccount teenAccount = new LoginAccountQuery.LoginAccount(
                "teen-oauth@test.com", null, AccountRole.TEEN, false, "Juca");
        when(loginAccountQuery.findForLogin("teen-oauth@test.com")).thenReturn(java.util.Optional.of(teenAccount));

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.TRILHA, destination);
    }

    // ---- OAuth principal com match GUARDIAN vinculado → /painel ----

    @Test
    @DisplayName("oauthPrincipalComMatchGuardianVinculadoRetornaPainel")
    void oauthPrincipalComMatchGuardianVinculadoRetornaPainel() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("email")).thenReturn("guardian-oauth@test.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        LoginAccountQuery.LoginAccount guardianAccount = new LoginAccountQuery.LoginAccount(
                "guardian-oauth@test.com", null, AccountRole.GUARDIAN, true, "Maria");
        when(loginAccountQuery.findForLogin("guardian-oauth@test.com")).thenReturn(java.util.Optional.of(guardianAccount));

        PostLoginDestination destination = handler.resolveDestination(authentication);

        assertEquals(PostLoginDestination.PAINEL, destination);
    }

    // ---- OAuth sem email → /login?error ----

    @Test
    @DisplayName("oauthSemEmailRetornaError")
    void oauthSemEmailRetornaError() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("email")).thenReturn(null);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

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
                "guardian2@test.com", "$2a$10$hash", AccountRole.GUARDIAN, false, "João");
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

    // ---- extractUsername para OAuth ----

    @Test
    @DisplayName("extractUsernameParaOAuth")
    void extractUsernameParaOAuth() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("email")).thenReturn("  OAuth@TEST.COM  ");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        assertEquals("oauth@test.com", handler.extractUsername(authentication));
    }
}
