package dev.zayt.atrilha.auth.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitedAuthenticationFailureHandlerTest {

    private LoginAttemptService loginAttemptService;
    private RateLimitedAuthenticationFailureHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        loginAttemptService = mock(LoginAttemptService.class);
        handler = new RateLimitedAuthenticationFailureHandler(loginAttemptService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    // ---- Critério: falha normal → /login?error ----

    @Test
    @DisplayName("falhaNormalRedirecionaParaError")
    void falhaNormalRedirecionaParaError() throws IOException {
        when(request.getParameter("username")).thenReturn("user@test.com");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(loginAttemptService.isBlocked(any(LoginAttemptKey.class))).thenReturn(false);

        AuthenticationException exception = new UsernameNotFoundException("bad credentials");

        handler.onAuthenticationFailure(request, response, exception);

        verify(loginAttemptService).registerFailure(any(LoginAttemptKey.class));
        verify(response).sendRedirect("/login?error");
    }

    // ---- Critério: bloqueado → /login?blocked ----

    @Test
    @DisplayName("bloqueadoRedirecionaParaBlocked")
    void bloqueadoRedirecionaParaBlocked() throws IOException {
        when(request.getParameter("username")).thenReturn("user@test.com");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");

        LoginAttemptKey key = LoginAttemptKey.of("user@test.com", "10.0.0.2");
        when(loginAttemptService.isBlocked(eq(key))).thenReturn(true);

        AuthenticationException exception = new UsernameNotFoundException("bad credentials");

        handler.onAuthenticationFailure(request, response, exception);

        verify(loginAttemptService).registerFailure(eq(key));
        verify(response).sendRedirect("/login?blocked");
    }

    // ---- Critério: LockedException → /login?blocked (sem registrar falha) ----

    @Test
    @DisplayName("lockedExceptionRedirecionaParaBlocked")
    void lockedExceptionRedirecionaParaBlocked() throws IOException {
        AuthenticationException exception = new LockedException("Account locked");

        handler.onAuthenticationFailure(request, response, exception);

        // Não deve registrar falha nem ler username/IP
        verify(loginAttemptService, never()).registerFailure(any());
        verify(response).sendRedirect("/login?blocked");
    }

    // ---- Critério: sem username → /login?error (sem registrar falha) ----

    @Test
    @DisplayName("semUsernameRedirecionaParaError")
    void semUsernameRedirecionaParaError() throws IOException {
        when(request.getParameter("username")).thenReturn(null);

        AuthenticationException exception = new UsernameNotFoundException("bad credentials");

        handler.onAuthenticationFailure(request, response, exception);

        verify(loginAttemptService, never()).registerFailure(any());
        verify(response).sendRedirect("/login?error");
    }

    // ---- Critério: username vazio → /login?error (sem registrar falha) ----

    @Test
    @DisplayName("usernameVazioRedirecionaParaError")
    void usernameVazioRedirecionaParaError() throws IOException {
        when(request.getParameter("username")).thenReturn("");

        AuthenticationException exception = new UsernameNotFoundException("bad credentials");

        handler.onAuthenticationFailure(request, response, exception);

        verify(loginAttemptService, never()).registerFailure(any());
        verify(response).sendRedirect("/login?error");
    }

    // ---- Critério: username normalizado (trim + lowercase) na chave ----

    @Test
    @DisplayName("usernameNormalizadoNaChave")
    void usernameNormalizadoNaChave() throws IOException {
        when(request.getParameter("username")).thenReturn("  User@TEST.COM  ");
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");

        // A chave deve ser normalizada: email trim + lowercase
        LoginAttemptKey expectedKey = LoginAttemptKey.of("  User@TEST.COM  ", "10.0.0.3");
        when(loginAttemptService.isBlocked(eq(expectedKey))).thenReturn(false);

        AuthenticationException exception = new UsernameNotFoundException("bad credentials");

        handler.onAuthenticationFailure(request, response, exception);

        verify(loginAttemptService).registerFailure(eq(expectedKey));
    }
}
