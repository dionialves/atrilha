package dev.zayt.atrilha.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Classifica falhas OAuth do Google em codigos amigaveis e redireciona Julia
 * de volta para a tela de escolha de metodo com {@code ?error=...} para
 * exibir toast apropriado (US-002 / UX spec §3.5).
 *
 * <p>Tabela de classificacao:
 * <ul>
 *   <li>{@code access_denied} ou {@code user_cancelled_login} →
 *       {@code ?error=cancelled} (toast info — Julia cancelou no Google)</li>
 *   <li>{@code email_unverified} (lancado por
 *       {@link GoogleOAuth2UserService}) → {@code ?error=email_unverified}
 *       (toast warning — Julia precisa verificar e-mail no Google)</li>
 *   <li>Qualquer outro erro → {@code ?error=oauth} (toast generico)</li>
 * </ul>
 * </p>
 *
 * <p>Visibilidade package-private; resolvido pelo {@code SecurityConfig} via DI.</p>
 */
@Component
class OAuthFailureHandler implements AuthenticationFailureHandler {

    private static final String REDIRECT_BASE =
            "/cadastro/adolescente/escolher-metodo?error=";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String code = classify(exception);
        response.sendRedirect(REDIRECT_BASE + code);
    }

    private static String classify(AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException oae) {
            String errCode = oae.getError() != null ? oae.getError().getErrorCode() : null;
            if ("access_denied".equals(errCode) || "user_cancelled_login".equals(errCode)) {
                return "cancelled";
            }
            if ("email_unverified".equals(errCode)) {
                return "email_unverified";
            }
        }
        return "oauth";
    }
}
