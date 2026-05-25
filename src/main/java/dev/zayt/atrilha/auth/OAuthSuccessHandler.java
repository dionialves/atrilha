package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.accounts.AccountReader;
import dev.zayt.atrilha.auth.login.AtrilhaOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;

/**
 * Dispatcher do callback OAuth Google (US-002 / Issue #37).
 *
 * <p>Fluxo:
 * <ol>
 *   <li>Extrai e-mail, given_name e picture do
 *       {@link OAuth2AuthenticationToken}.</li>
 *   <li>Normaliza e-mail (trim + lowercase).</li>
 *   <li>Se ja existe {@link dev.zayt.atrilha.accounts.Account} com esse
 *       e-mail (case-insensitive, ignorando soft-delete) → limpa o
 *       SecurityContext e redireciona a Tela 2 com
 *       {@code ?error=account_exists}. Julia <strong>nao</strong> deve ficar
 *       "autenticada como ela mesma" antes de logar — esta US so cobre
 *       cadastro novo. Login Google e US-008/US-009.</li>
 *   <li>Caso contrario: cria {@link PendingGoogleSignup}, guarda na sessao
 *       e redireciona para a Tela 3 ({@code /cadastro/adolescente/complementar}).
 *       Tambem limpa o SecurityContext — Julia so fica autenticada apos o
 *       POST de complementacao via {@link SessionAuthenticator}.</li>
 * </ol>
 * </p>
 *
 * <p>Visibilidade package-private; resolvido pelo {@code SecurityConfig} via DI.</p>
 */
@Component
class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    static final String SESSION_KEY = "pendingGoogleSignup";
    private static final String REDIRECT_COMPLEMENTAR = "/cadastro/adolescente/complementar";
    private static final String REDIRECT_CONFLICT =
            "/cadastro/adolescente/escolher-metodo?error=account_exists";

    private static final Logger log = LoggerFactory.getLogger(OAuthSuccessHandler.class);

    private final AccountReader accountReader;
    private final Clock clock;

    OAuthSuccessHandler(AccountReader accountReader, Clock clock) {
        this.accountReader = accountReader;
        this.clock = clock;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // Extrai atributos OAuth do principal (AtrilhaOAuth2User em PENDING_SIGNUP)
        // ou do token original. O dispatcher garante que so OAuth2AuthenticationToken
        // chegue aqui com principal AtrilhaOAuth2User pendente.
        Map<String, Object> attrs;
        if (authentication.getPrincipal() instanceof AtrilhaOAuth2User user && user.isPendingSignup()) {
            attrs = user.getAttributes();
        } else if (authentication instanceof OAuth2AuthenticationToken token) {
            attrs = token.getPrincipal().getAttributes();
        } else {
            throw new ClassCastException(
                    "OAuthSuccessHandler invocado com principal inesperado: "
                            + authentication.getPrincipal().getClass().getName());
        }

        String rawEmail = (String) attrs.get("email");
        if (rawEmail == null) {
            log.warn("oauth_google_callback missing email claim");
            SecurityContextHolder.clearContext();
            response.sendRedirect("/cadastro/adolescente/escolher-metodo?error=oauth");
            return;
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);

        if (accountReader.existsByEmailIgnoreCase(email)) {
            log.info("oauth_google_callback conflict — account already exists");
            SecurityContextHolder.clearContext();
            response.sendRedirect(REDIRECT_CONFLICT);
            return;
        }

        Instant now = Instant.now(clock);
        PendingGoogleSignup pending = new PendingGoogleSignup(
                email,
                OffsetDateTime.ofInstant(now, ZoneOffset.UTC),
                (String) attrs.getOrDefault("given_name", ""),
                (String) attrs.get("picture"),
                now);
        request.getSession(true).setAttribute(SESSION_KEY, pending);

        // Julia ainda nao e "autenticada como ela mesma" — so depois da
        // complementacao + SessionAuthenticator. Limpar contexto evita que
        // outras requests durante a complementacao vejam ela como TEEN
        // antes da hora.
        SecurityContextHolder.clearContext();
        response.sendRedirect(REDIRECT_COMPLEMENTAR);
    }
}
