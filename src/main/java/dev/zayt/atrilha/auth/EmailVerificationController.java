package dev.zayt.atrilha.auth;

import dev.zayt.atrilha.accounts.Account;
import dev.zayt.atrilha.accounts.AccountReader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * Controller real do fluxo de verificação de e-mail (US-006).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /verificar-email} — tela "Confirme teu e-mail" (autenticado).
 *       Se a conta já estiver verificada, redireciona para {@code /}.</li>
 *   <li>{@code POST /verificar-email/reenviar} — reenvio com cooldown 60s e
 *       limite 5/hora (autenticado, CSRF). Sempre redireciona para
 *       {@code GET /verificar-email} carregando flash {@code resendStatus}.</li>
 *   <li>{@code GET /verify-email} — endpoint público chamado pelo link do
 *       e-mail. Renderiza a tela de resultado com {@code outcome} no modelo
 *       ({@code SUCCESS}, {@code EXPIRED_OR_INVALID} ou {@code ALREADY_USED}).</li>
 * </ul>
 * </p>
 *
 * <p>Decisão de roteamento pós-SUCCESS: a tela de resultado mostra um CTA
 * "Continuar" apontando para {@code /} (home). A US-007 (login) ainda não
 * existe no Sprint 3, então não há tela {@code /login} para mensagens —
 * mantemos o fluxo simples. A sessão do usuário, se já logado, é atualizada
 * automaticamente porque {@code emailVerifiedAt} é lido do banco no
 * {@code @ControllerAdvice} a cada request (banner some sozinho).</p>
 *
 * <p>Visibilidade package-private — Spring registra via {@code @Controller}.</p>
 */
@Controller
class EmailVerificationController {

    private static final String VIEW_PENDING = "verificar-email";
    private static final String VIEW_RESULT = "verify-email-resultado";
    private static final String REDIRECT_PENDING = "redirect:/verificar-email";
    private static final String REDIRECT_HOME = "redirect:/";

    private final EmailVerificationService verificationService;
    private final AccountReader accountReader;

    EmailVerificationController(EmailVerificationService verificationService,
                                AccountReader accountReader) {
        this.verificationService = verificationService;
        this.accountReader = accountReader;
    }

    @GetMapping("/verificar-email")
    String renderPending(Model model) {
        Optional<Account> currentOpt = currentAccount();
        if (currentOpt.isEmpty()) {
            // Não deveria acontecer (Spring Security barra antes), mas defensivo.
            return REDIRECT_HOME;
        }
        Account current = currentOpt.get();
        if (current.getEmailVerifiedAt() != null) {
            return REDIRECT_HOME;
        }
        model.addAttribute("email", current.getEmail());
        return VIEW_PENDING;
    }

    @PostMapping("/verificar-email/reenviar")
    String resend(RedirectAttributes redirect) {
        Optional<Account> currentOpt = currentAccount();
        if (currentOpt.isEmpty()) {
            return REDIRECT_HOME;
        }
        Account current = currentOpt.get();
        if (current.getEmailVerifiedAt() != null) {
            return REDIRECT_HOME;
        }
        try {
            verificationService.resend(current);
            redirect.addFlashAttribute("resendStatus", "success");
        } catch (EmailResendRateLimitedException e) {
            redirect.addFlashAttribute("resendStatus", "rate_limited");
            redirect.addFlashAttribute("resendRetryAfter", e.getRetryAfterSeconds());
        }
        return REDIRECT_PENDING;
    }

    @GetMapping("/verify-email")
    String verify(@RequestParam(name = "token", required = false) String tokenParam,
                  Model model) {
        UUID token = parseUuid(tokenParam);
        VerificationResult outcome = verificationService.verify(token);
        model.addAttribute("outcome", outcome.name());
        return VIEW_RESULT;
    }

    private Optional<Account> currentAccount() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AuthenticatedAccount principal)) {
            return Optional.empty();
        }
        return accountReader.findById(principal.id());
    }

    /**
     * Converte string em UUID, retornando {@code null} em qualquer falha —
     * a UI mostra a mesma tela "link inválido ou expirado", evitando
     * vazamento de estado (UX spec §5.3, privacidade).
     */
    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
