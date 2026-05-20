package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.accounts.Account;
import dev.zayt.atrilha.accounts.AccountReader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Enforcement runtime de {@link RequiresVerifiedEmail} (US-006 critério 5 +
 * UX spec §8).
 *
 * <p>Estratégia:
 * <ul>
 *   <li>Se o handler não é {@link HandlerMethod} → passa (handlers estáticos
 *       como ResourceHandler, ErrorController etc. nunca consomem a anotação).</li>
 *   <li>Se nem o método nem a classe estão anotados → passa.</li>
 *   <li>Se o usuário não está autenticado → passa (Spring Security já barra
 *       antes em endpoints protegidos; em endpoints públicos a anotação não
 *       faz sentido).</li>
 *   <li>Se a conta tem {@code emailVerifiedAt != null} → passa.</li>
 *   <li>Se a conta não foi verificada e o método é GET → redirect 302 para
 *       {@code /verificar-email}.</li>
 *   <li>Caso contrário (POST/PUT/DELETE) → 403.</li>
 * </ul>
 * </p>
 *
 * <p>Visibilidade package-private; consumido apenas via Spring DI pelo
 * {@code AuthWebMvcConfig}.</p>
 */
@Component
class RequiresVerifiedEmailInterceptor implements HandlerInterceptor {

    private final AccountReader accountReader;

    RequiresVerifiedEmailInterceptor(AccountReader accountReader) {
        this.accountReader = accountReader;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean annotated =
                handlerMethod.getMethodAnnotation(RequiresVerifiedEmail.class) != null
                || handlerMethod.getBeanType().isAnnotationPresent(RequiresVerifiedEmail.class);
        if (!annotated) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AuthenticatedAccount principal)) {
            // Sem sessão real — não é responsabilidade desta camada barrar acesso
            // anônimo; Spring Security já o fez (ou intencionalmente liberou).
            return true;
        }

        Optional<Account> accountOpt = accountReader.findById(principal.id());
        if (accountOpt.isPresent() && accountOpt.get().getEmailVerifiedAt() != null) {
            return true;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            response.sendRedirect("/verificar-email");
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
        return false;
    }
}
