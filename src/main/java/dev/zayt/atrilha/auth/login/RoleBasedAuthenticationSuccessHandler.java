package dev.zayt.atrilha.auth.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import dev.zayt.atrilha.auth.AccountRole;

import java.io.IOException;
import java.util.Locale;

/**
 * {@link AuthenticationSuccessHandler} baseado em papel (US-007).
 *
 * <p>Resolve o destino pós-login conforme o tipo de autenticação:
 * <ul>
 *   <li><b>Form login</b> → {@link AtrilhaUserDetails} no principal:
 *       <ul>
 *         <li>TEEN → {@code /trilha}</li>
 *         <li>GUARDIAN com vínculo → {@code /painel}</li>
 *         <li>GUARDIAN sem vínculo → {@code /vincular}</li>
 *       </ul>
 *   </li>
 *   <li><b>OAuth Google</b> → {@link OAuth2User} no principal:
 *       extrai email, consulta {@link LoginAccountQuery#findForLogin}, aplica mesma lógica acima.
 *       Se email não encontrado → {@code /login?error}.</li>
 * </ul>
 *
 * <p>Registra sucesso no rate-limit service para limpar contadores.</p>
 */
@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(RoleBasedAuthenticationSuccessHandler.class);
    private static final String REDIRECT_ERROR = "/login?error";

    private final LoginAccountQuery loginAccountQuery;
    private final LoginAttemptService loginAttemptService;

    RoleBasedAuthenticationSuccessHandler(LoginAccountQuery loginAccountQuery,
                                          LoginAttemptService loginAttemptService) {
        this.loginAccountQuery = loginAccountQuery;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        PostLoginDestination destination = resolveDestination(authentication);

        // Registra sucesso no rate-limit (limpa contadores da chave IP+email)
        String username = extractUsername(authentication);
        if (username != null && !username.isBlank()) {
            LoginAttemptKey key = LoginAttemptKey.of(username, request.getRemoteAddr());
            loginAttemptService.registerSuccess(key);
        }

        log.info("auth.login.success destination={} ip={}",
                destination.path(), hashIp(request.getRemoteAddr()));

        response.sendRedirect(destination.path());
    }

    /**
     * Resolve o destino pós-login a partir do tipo de {@link Authentication}.
     */
    PostLoginDestination resolveDestination(Authentication authentication) {
        if (authentication.getPrincipal() instanceof AtrilhaUserDetails atrilha) {
            // Form login — principal já é nosso UserDetails customizado
            return resolveForRole(atrilha.getRole(), atrilha.hasGuardianLink());
        }

        if (authentication.getPrincipal() instanceof OAuth2User oauth2) {
            // OAuth Google — extrai email do atributo e consulta a conta
            String rawEmail = (String) oauth2.getAttribute("email");
            if (rawEmail == null || rawEmail.isBlank()) {
                log.warn("auth.login.oauth_missing_email");
                return PostLoginDestination.valueOfError(); // fallback — não existe
            }

            String email = rawEmail.trim().toLowerCase(Locale.ROOT);
            return loginAccountQuery.findForLogin(email)
                    .map(account -> resolveForRole(account.role(), account.hasGuardianLink()))
                    .orElseGet(() -> {
                        log.info("auth.login.oauth_unknown_email ip={}", hashIp(null));
                        return PostLoginDestination.valueOfError(); // não encontrado
                    });
        }

        // Tipo inesperado — erro genérico
        log.warn("auth.login.unexpected_principal_type={}", authentication.getPrincipal().getClass().getName());
        return PostLoginDestination.valueOfError();
    }

    /**
     * Mapeia papel + estado de vínculo para o destino correto.
     */
    PostLoginDestination resolveForRole(AccountRole role, boolean hasGuardianLink) {
        if (role == AccountRole.TEEN) {
            return PostLoginDestination.TRILHA;
        }
        // GUARDIAN
        if (hasGuardianLink) {
            return PostLoginDestination.PAINEL;
        }
        return PostLoginDestination.VINCULAR;
    }

    /** Extrai o username (email) da Authentication para rate-limit. */
    String extractUsername(Authentication authentication) {
        if (authentication.getPrincipal() instanceof AtrilhaUserDetails atrilha) {
            return atrilha.getUsername();
        }
        if (authentication.getPrincipal() instanceof OAuth2User oauth2) {
            String email = (String) oauth2.getAttribute("email");
            return email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
        }
        return null;
    }

    private static String hashIp(String ip) {
        if (ip == null || ip.isBlank()) return "unknown";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(ip.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 8);
        } catch (java.security.NoSuchAlgorithmException e) {
            return "hash_error";
        }
    }
}
