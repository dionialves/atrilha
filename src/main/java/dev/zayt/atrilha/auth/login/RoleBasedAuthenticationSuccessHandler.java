package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.AuthenticatedPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * {@link AuthenticationSuccessHandler} baseado em papel (US-007).
 *
 * <p>Resolve o destino pós-login conforme o papel do principal autenticado:
 * <ul>
 *   <li><b>Form login</b> → {@link AtrilhaUserDetails} no principal:
 *       <ul>
 *         <li>TEEN → {@code /trilha}</li>
 *         <li>GUARDIAN com vínculo → {@code /painel}</li>
 *         <li>GUARDIAN sem vínculo → {@code /vincular}</li>
 *       </ul>
 *   </li>
 *   <li><b>OAuth Google</b> → {@link AtrilhaOAuth2User} no principal:
 *       mesma lógica acima — authorities derivadas da conta no banco pelo
 *       {@code GoogleOAuth2UserService}.</li>
 * </ul>
 *
 * <p>Registra sucesso no rate-limit service para limpar contadores.</p>
 */
@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(RoleBasedAuthenticationSuccessHandler.class);
    private static final String REDIRECT_ERROR = "/login?error";

    private final LoginAttemptService loginAttemptService;

    RoleBasedAuthenticationSuccessHandler(LoginAttemptService loginAttemptService) {
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
     *
     * <p>Após o FIX-014, todo principal de login (form ou OAuth) implementa
     * {@link AuthenticatedPrincipal}, eliminando a necessidade de branch por tipo.</p>
     *
     * <p>Se o principal for um {@code AtrilhaOAuth2User} em estado PENDING_SIGNUP,
     * retorna {@code ERROR} — o dispatcher deve impedir que esse caso chegue aqui.</p>
     */
    PostLoginDestination resolveDestination(Authentication authentication) {
        if (authentication.getPrincipal() instanceof AuthenticatedPrincipal p) {
            // Defesa: pending signup nunca deve chegar aqui (dispatcher encaminha para OAuthSuccessHandler)
            if (p instanceof dev.zayt.atrilha.auth.login.AtrilhaOAuth2User u && u.isPendingSignup()) {
                log.warn("auth.login.pending_signup_reached_role_based_handler — dispatcher falhou");
                return PostLoginDestination.ERROR;
            }
            return resolveForRole(p.role(), p.hasGuardianLink());
        }

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
        if (authentication.getPrincipal() instanceof AuthenticatedPrincipal p) {
            return p.getAccount().email();
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
