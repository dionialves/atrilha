package dev.zayt.atrilha.auth.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * {@link AuthenticationFailureHandler} com rate-limit (US-007).
 *
 * <p>Fluxo:
 * <ol>
 *   <li>Extrai {@code username} e {@code remoteAddr} do request.</li>
 *   <li>Cria {@link LoginAttemptKey} e consulta {@link LoginAttemptService#isBlocked}.</li>
 *   <li>Se bloqueado → 302 para {@code /login?blocked}.</li>
 *   <li>Se não bloqueado → 302 para {@code /login?error}.</li>
 *   <li>{@link LockedException} é sempre mapeado para {@code ?blocked}.</li>
 * </ol>
 *
 * <p><strong>NUNCA</strong> loga e-mail em claro, senha ou token.</p>
 */
@Component
class RateLimitedAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(RateLimitedAuthenticationFailureHandler.class);
    private static final String REDIRECT_BLOCKED = "/login?blocked";
    private static final String REDIRECT_ERROR = "/login?error";

    private final LoginAttemptService loginAttemptService;

    RateLimitedAuthenticationFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        // LockedException = conta bloqueada (não rate-limit) → sempre ?blocked
        if (exception instanceof LockedException) {
            log.info("auth.login.locked_account");
            response.sendRedirect(REDIRECT_BLOCKED);
            return;
        }

        // Extrai username e IP do request (pode vir vazio em OAuth failures)
        String username = request.getParameter("username");
        String ip = request.getRemoteAddr();

        if (username == null || username.isBlank()) {
            // Sem username — não podemos rate-limitar, redireciona como erro genérico
            response.sendRedirect(REDIRECT_ERROR);
            return;
        }

        LoginAttemptKey key = LoginAttemptKey.of(username, ip);

        // Registra a falha (incrementa contador / bloqueia se necessário)
        loginAttemptService.registerFailure(key);

        // Decide o redirecionamento com base no estado atual
        if (loginAttemptService.isBlocked(key)) {
            log.info("auth.login.failure ip={}", hashIp(ip));
            response.sendRedirect(REDIRECT_BLOCKED);
        } else {
            log.info("auth.login.failure ip={}", hashIp(ip));
            response.sendRedirect(REDIRECT_ERROR);
        }
    }

    private static String hashIp(String ip) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(ip.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 8); // apenas os primeiros 8 hex chars
        } catch (NoSuchAlgorithmException e) {
            return "hash_error";
        }
    }
}
