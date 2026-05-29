package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.accounts.repository.AccountRepository;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import dev.zayt.atrilha.auth.domain.PasswordResetResult;
import dev.zayt.atrilha.auth.session.SessionAuthenticator;
import dev.zayt.atrilha.auth.verification.PasswordResetService;
import dev.zayt.atrilha.auth.verification.PasswordResetToken;
import dev.zayt.atrilha.auth.verification.PasswordResetTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fluxo de consumo do link de redefinição de senha (US-008-d).
 *
 * <p>Expõe GET/POST {@code /reset-senha} para usuários não autenticados que
 * chegam pelo link enviado em US-008-c. O fluxo:
 * <ol>
 *   <li>GET — verifica o estado do token sem consumi-lo e renderiza
 *       formulário (SUCCESS) ou tela de erro (EXPIRED_OR_INVALID / ALREADY_USED).</li>
 *   <li>POST — valida senha mínima (8 chars), consome o token atomicamente
 *       via {@link PasswordResetService#verify(UUID)}, persiste novo
 *       {@code passwordHash} (BCrypt 12), invalida sessões pré-existentes
 *       (CA-4) e estabelece sessão autenticada.</li>
 * </ol>
 * </p>
 *
 * <p><b>Por que ler o token via {@link PasswordResetTokenRepository#findByToken}
 * no GET, e não via {@link PasswordResetService#verify}:</b> {@code verify} é
 * destrutivo — em SUCCESS marca {@code usedAt = now} e devolve o resultado.
 * Em GET precisamos apenas decidir qual tela mostrar (sem consumir).</p>
 *
 * <p><b>Segurança — não confiamos no accountId do form:</b> o link de reset
 * só carrega o token. O accountId é derivado server-side do
 * {@link PasswordResetToken} após o {@code verify} retornar SUCCESS, evitando
 * que um atacante forje o ID de outra conta.</p>
 *
 * <p>Visibilidade package-private — Spring registra via {@code @Controller}.</p>
 */
@Controller
@RequestMapping("/reset-senha")
class PasswordResetController {

    private static final String VIEW_FORM = "auth/reset-senha";
    private static final String REDIRECT_HOME = "redirect:/";
    private static final String OUTCOME_EXPIRED = PasswordResetResult.EXPIRED_OR_INVALID.name();
    private static final String OUTCOME_USED = PasswordResetResult.ALREADY_USED.name();
    private static final String OUTCOME_SUCCESS = PasswordResetResult.SUCCESS.name();

    private final PasswordResetService passwordResetService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionAuthenticator sessionAuthenticator;
    private final SessionRegistry sessionRegistry;
    private final Clock clock;

    PasswordResetController(PasswordResetService passwordResetService,
                            PasswordResetTokenRepository passwordResetTokenRepository,
                            AccountRepository accountRepository,
                            PasswordEncoder passwordEncoder,
                            SessionAuthenticator sessionAuthenticator,
                            SessionRegistry sessionRegistry,
                            Clock clock) {
        this.passwordResetService = passwordResetService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionAuthenticator = sessionAuthenticator;
        this.sessionRegistry = sessionRegistry;
        this.clock = clock;
    }

    @GetMapping
    String showForm(@RequestParam(name = "token", required = false) String tokenParam,
                    @ModelAttribute("resetForm") ResetForm form,
                    Model model) {
        UUID tokenValue = parseUuid(tokenParam);
        String outcome = peekOutcome(tokenValue);
        model.addAttribute("outcome", outcome);
        if (OUTCOME_SUCCESS.equals(outcome) && tokenValue != null) {
            model.addAttribute("token", tokenValue.toString());
            form.setToken(tokenValue.toString());
        }
        return VIEW_FORM;
    }

    @PostMapping
    String submit(@Validated @ModelAttribute("resetForm") ResetForm form,
                  BindingResult bindingResult,
                  Model model,
                  HttpServletRequest request,
                  HttpServletResponse response) {
        UUID tokenValue = parseUuid(form.getToken());

        // 1. Verifica estado do token SEM consumir (read-only peek).
        String preOutcome = peekOutcome(tokenValue);
        if (!OUTCOME_SUCCESS.equals(preOutcome)) {
            model.addAttribute("outcome", preOutcome);
            return VIEW_FORM;
        }

        // 2. Validação de senha (mínimo 8 caracteres). Token preservado em campo
        //    hidden para que o usuário possa corrigir e resubmeter sem refazer GET.
        if (bindingResult.hasErrors()) {
            model.addAttribute("outcome", OUTCOME_SUCCESS);
            model.addAttribute("token", form.getToken());
            return VIEW_FORM;
        }

        // 3. Consome o token atomicamente. Defesa contra race: outro request
        //    pode ter consumido entre peek e verify; verify devolve o estado real.
        PasswordResetResult result = passwordResetService.verify(tokenValue);
        if (result != PasswordResetResult.SUCCESS) {
            model.addAttribute("outcome", result.name());
            return VIEW_FORM;
        }

        // 4. Recupera a conta associada ao token (a partir do registro do token,
        //    NÃO de um campo do form — defesa contra tampering).
        Optional<PasswordResetToken> tokenEntityOpt = passwordResetTokenRepository.findByToken(tokenValue);
        if (tokenEntityOpt.isEmpty()) {
            // Defensivo: não deveria ocorrer (verify acabou de tocar a linha).
            model.addAttribute("outcome", OUTCOME_EXPIRED);
            return VIEW_FORM;
        }
        UUID accountId = tokenEntityOpt.get().getAccountId();

        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            // Conta sumiu entre emissão e consumo — sem caminho seguro de prosseguir.
            model.addAttribute("outcome", OUTCOME_EXPIRED);
            return VIEW_FORM;
        }
        Account account = accountOpt.get();

        // 5. Persiste novo hash BCrypt (rounds=12 configurado em SecurityConfig).
        account.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        accountRepository.save(account);

        // 6. Invalida sessões pré-existentes (CA-4) ANTES de criar a nova.
        AccountRole role = toRole(account);
        invalidatePreviousSessions(account.getId(), role);

        // 7. Estabelece sessão autenticada (auto-login).
        sessionAuthenticator.authenticate(request, response, account.getId(), role);

        return REDIRECT_HOME;
    }

    /**
     * Determina o {@code outcome} do token SEM consumi-lo. Espelha a lógica de
     * {@link PasswordResetService#verify(UUID)} usando o lookup read-only
     * {@link PasswordResetTokenRepository#findByToken(UUID)}.
     */
    private String peekOutcome(UUID tokenValue) {
        if (tokenValue == null) {
            return OUTCOME_EXPIRED;
        }
        Optional<PasswordResetToken> entityOpt = passwordResetTokenRepository.findByToken(tokenValue);
        if (entityOpt.isEmpty()) {
            return OUTCOME_EXPIRED;
        }
        PasswordResetToken entity = entityOpt.get();
        if (entity.getUsedAt() != null) {
            return OUTCOME_USED;
        }
        if (clock.instant().isAfter(entity.getExpiresAt())) {
            return OUTCOME_EXPIRED;
        }
        return OUTCOME_SUCCESS;
    }

    /**
     * Invalida (expira) todas as sessões ativas pré-existentes do usuário.
     * CA-4 da US-008: comprometimento de senha exige que sessões em outros
     * dispositivos deixem de ser válidas.
     */
    private void invalidatePreviousSessions(UUID accountId, AccountRole role) {
        AuthenticatedAccount principal = new AuthenticatedAccount(accountId, role);
        List<SessionInformation> activeSessions = sessionRegistry.getAllSessions(principal, false);
        for (SessionInformation info : activeSessions) {
            info.expireNow();
        }
    }

    private static AccountRole toRole(Account account) {
        return "ADOLESCENT".equals(account.getType()) ? AccountRole.TEEN : AccountRole.GUARDIAN;
    }

    /**
     * Converte string em UUID, retornando {@code null} em qualquer falha —
     * a UI mostra a mesma tela "link inválido ou expirado", sem vazar estado.
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

    /**
     * Form bean — token vem como hidden field; newPassword vem do input do usuário.
     */
    static class ResetForm {

        @NotBlank
        private String token;

        @NotBlank(message = "{password.reset.form.password.required}")
        @Size(min = 8, message = "{password.reset.form.password.min}")
        private String newPassword;

        public ResetForm() {
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
