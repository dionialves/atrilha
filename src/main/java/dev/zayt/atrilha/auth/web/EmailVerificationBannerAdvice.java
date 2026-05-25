package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.accounts.repository.AccountReader;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expõe os atributos do banner de verificação de e-mail ao modelo de toda
 * requisição Thymeleaf (US-006 §7).
 *
 * <p>Atributos:
 * <ul>
 *   <li>{@code unverifiedEmail} — {@code true} apenas quando há
 *       {@link AuthenticatedAccount} no contexto e a conta correspondente
 *       tem {@code emailVerifiedAt == null}.</li>
 *   <li>{@code showEmailVerificationBanner} — variante mais restrita usada
 *       diretamente pelo fragment: verdadeiro apenas quando
 *       {@code unverifiedEmail} também é verdadeiro <strong>e</strong> a URI
 *       não pertence ao próprio fluxo de verificação (UX spec §7.1).</li>
 * </ul>
 * </p>
 *
 * <p>Visibilidade package-private; o Spring instancia via DI e o
 * {@code @ControllerAdvice} registra automaticamente.</p>
 */
@ControllerAdvice
public class EmailVerificationBannerAdvice {

    private final AccountReader accountReader;

    public EmailVerificationBannerAdvice(AccountReader accountReader) {
        this.accountReader = accountReader;
    }

    @ModelAttribute("unverifiedEmail")
    public boolean unverifiedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AuthenticatedAccount principal)) {
            return false;
        }
        return accountReader.findById(principal.id())
                .map(a -> a.getEmailVerifiedAt() == null)
                .orElse(false);
    }

    @ModelAttribute("showEmailVerificationBanner")
    public boolean showEmailVerificationBanner(HttpServletRequest request) {
        if (!unverifiedEmail()) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        if (uri.equals("/verificar-email") || uri.startsWith("/verificar-email/")) {
            return false;
        }
        if (uri.startsWith("/verify-email")) {
            return false;
        }
        return true;
    }
}
