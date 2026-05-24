package dev.zayt.atrilha.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link OAuthFailureHandler} classifica erros OAuth e redireciona para a tela
 * de escolha de metodo com {@code ?error=...}.
 */
class OAuthFailureHandlerTest {

    private final OAuthFailureHandler handler = new OAuthFailureHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    void classifyAccessDeniedRetornaCancelled() throws IOException {
        OAuth2AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("access_denied", "User cancelled", null));

        handler.onAuthenticationFailure(request, response, ex);

        verify(response).sendRedirect("/cadastro/adolescente/escolher-metodo?error=cancelled");
    }

    @Test
    void classifyUserCancelledLoginRetornaCancelled() throws IOException {
        OAuth2AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("user_cancelled_login", "User cancelled", null));

        handler.onAuthenticationFailure(request, response, ex);

        verify(response).sendRedirect("/cadastro/adolescente/escolher-metodo?error=cancelled");
    }

    @Test
    void classifyEmailUnverifiedRetornaEmailUnverified() throws IOException {
        OAuth2AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("email_unverified", "Email not verified", null));

        handler.onAuthenticationFailure(request, response, ex);

        verify(response).sendRedirect("/cadastro/adolescente/escolher-metodo?error=email_unverified");
    }

    @Test
    void classifyAccountNotFoundRetornaNoAccount() throws IOException {
        OAuth2AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("account_not_found", "No account found", null));

        handler.onAuthenticationFailure(request, response, ex);

        verify(response).sendRedirect("/cadastro/adolescente/escolher-metodo?error=no_account");
    }

    @Test
    void classifyGenericErrorRetornaOauth() throws IOException {
        OAuth2AuthenticationException ex = new OAuth2AuthenticationException(
                new OAuth2Error("some_other_error", "Unknown error", null));

        handler.onAuthenticationFailure(request, response, ex);

        verify(response).sendRedirect("/cadastro/adolescente/escolher-metodo?error=oauth");
    }

    @Test
    void classifyNonOAuthExceptionRetornaOauth() throws IOException {
        OAuth2AuthenticationException ex = new OAuth2AuthenticationException("Generic error");

        handler.onAuthenticationFailure(request, response, ex);

        verify(response).sendRedirect("/cadastro/adolescente/escolher-metodo?error=oauth");
    }

    @Test
    void classifyNullErrorRetornaOauth() throws IOException {
        // OAuth2AuthenticationException com mensagem (sem OAuth2Error) — o errorCode sera null
        OAuth2AuthenticationException ex = new OAuth2AuthenticationException(
                "Erro generico de autenticacao");

        handler.onAuthenticationFailure(request, response, ex);

        verify(response).sendRedirect("/cadastro/adolescente/escolher-metodo?error=oauth");
    }

    @Test
    void classifyNonOAuth2ExceptionRetornaOauth() throws IOException {
        org.springframework.security.core.AuthenticationException ex =
                new org.springframework.security.core.AuthenticationException("Not OAuth") {};

        handler.onAuthenticationFailure(request, response, ex);

        verify(response).sendRedirect("/cadastro/adolescente/escolher-metodo?error=oauth");
    }
}
