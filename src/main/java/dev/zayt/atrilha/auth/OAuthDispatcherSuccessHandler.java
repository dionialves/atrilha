package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.auth.login.AtrilhaOAuth2User;
import dev.zayt.atrilha.auth.login.RoleBasedAuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dispatcher do callback OAuth Google. Decide se a request e um login
 * (conta existe -> delega para RoleBasedAuthenticationSuccessHandler) ou
 * um cadastro novo (conta nao existe -> delega para OAuthSuccessHandler).
 *
 * <p>Visibilidade package-private; resolvido pelo SecurityConfig via DI.</p>
 */
@Component
class OAuthDispatcherSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthSuccessHandler signupHandler;
    private final RoleBasedAuthenticationSuccessHandler loginHandler;

    OAuthDispatcherSuccessHandler(OAuthSuccessHandler signupHandler,
                                  RoleBasedAuthenticationSuccessHandler loginHandler) {
        this.signupHandler = signupHandler;
        this.loginHandler = loginHandler;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AtrilhaOAuth2User user && user.isPendingSignup()) {
            signupHandler.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        loginHandler.onAuthenticationSuccess(request, response, authentication);
    }
}
