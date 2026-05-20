package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.PendingGoogleSignup;
import dev.zayt.atrilha.auth.SessionAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

/**
 * Controller das telas exclusivas do cadastro adolescente via Google
 * (US-002 / Issue #37):
 * <ul>
 *   <li>{@code GET /cadastro/adolescente/escolher-metodo} (Tela 2)</li>
 *   <li>{@code GET/POST /cadastro/adolescente/complementar} (Tela 3)</li>
 * </ul>
 *
 * <p>Convive com {@code AdolescentRegistrationController} (US-001) sem
 * conflito — a rota raiz {@code /cadastro/adolescente} continua sendo o
 * form de e-mail/senha; aqui sao apenas sub-rotas.</p>
 *
 * <p>A detecao de "idade fora da faixa" segue o mesmo padrao do controller
 * US-001 (helper {@link #detectAgeBlockVariant}): bloqueio so quando o
 * unico erro e {@code @EligibleAge}, para nao revelar a regra interna em
 * erros compostos (CA-4 US-005).</p>
 */
@Controller
@RequestMapping("/cadastro/adolescente")
class AdolescentGoogleSignupController {

    private static final String SESSION_KEY = "pendingGoogleSignup";
    private static final String VIEW_ESCOLHER = "cadastro/adolescente_escolher_metodo";
    private static final String VIEW_COMPLEMENTAR = "cadastro/adolescente_complementar";
    private static final String VIEW_BLOCKED = "cadastro/adolescente_bloqueado";
    private static final String REDIRECT_ESCOLHER = "redirect:/cadastro/adolescente/escolher-metodo";
    private static final String REDIRECT_CONCLUIDO = "redirect:/cadastro/concluido";

    private final RegisterAdolescentService service;
    private final SessionAuthenticator sessionAuthenticator;
    private final Clock clock;

    AdolescentGoogleSignupController(RegisterAdolescentService service,
                                     SessionAuthenticator sessionAuthenticator,
                                     Clock clock) {
        this.service = service;
        this.sessionAuthenticator = sessionAuthenticator;
        this.clock = clock;
    }

    @GetMapping("/escolher-metodo")
    String escolherMetodo(@RequestParam(value = "error", required = false) String error,
                          Model model) {
        if (error != null) {
            model.addAttribute("error", error);
        }
        return VIEW_ESCOLHER;
    }

    @GetMapping("/complementar")
    String complementarForm(HttpSession session,
                            @RequestParam(value = "cancel", required = false) String cancel,
                            Model model) {
        if ("1".equals(cancel)) {
            session.removeAttribute(SESSION_KEY);
            return REDIRECT_ESCOLHER;
        }
        PendingGoogleSignup pending = (PendingGoogleSignup) session.getAttribute(SESSION_KEY);
        if (pending == null) {
            return REDIRECT_ESCOLHER;
        }
        if (!model.containsAttribute("form")) {
            CompleteGoogleSignupForm form = new CompleteGoogleSignupForm();
            form.setNickname(truncate(pending.givenName(), 20));
            form.setPhotoSource(pending.picture() != null && !pending.picture().isBlank()
                    ? CompleteGoogleSignupForm.PhotoSource.GOOGLE
                    : CompleteGoogleSignupForm.PhotoSource.NONE);
            model.addAttribute("form", form);
        }
        model.addAttribute("pending", pending);
        return VIEW_COMPLEMENTAR;
    }

    @PostMapping("/complementar")
    String complementarSubmit(@Valid @ModelAttribute("form") CompleteGoogleSignupForm form,
                              BindingResult bindingResult,
                              @RequestParam(value = "photo", required = false) MultipartFile photo,
                              HttpSession session,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              Model model) {
        PendingGoogleSignup pending = (PendingGoogleSignup) session.getAttribute(SESSION_KEY);
        if (pending == null) {
            return REDIRECT_ESCOLHER;
        }

        if (bindingResult.hasErrors()) {
            String ageBlockVariant = detectAgeBlockVariant(bindingResult);
            if (ageBlockVariant != null) {
                // CA-4 US-005: limpa sessao antes de mostrar bloqueio, para
                // que tentativas posteriores reiniciem o fluxo do zero.
                session.removeAttribute(SESSION_KEY);
                model.addAttribute("variant", ageBlockVariant);
                return VIEW_BLOCKED;
            }
            model.addAttribute("pending", pending);
            return VIEW_COMPLEMENTAR;
        }

        CompleteGoogleSignupRequest req = new CompleteGoogleSignupRequest(
                form.getNickname(), form.getBirthDate(), form.getPhotoSource());
        RegisterAdolescentService.Outcome outcome = service.registerFromGoogle(pending, req, photo);

        return switch (outcome) {
            case RegisterAdolescentService.Outcome.GoogleRegistered g -> {
                session.removeAttribute(SESSION_KEY);
                sessionAuthenticator.authenticate(request, response, g.accountId(), AccountRole.TEEN);
                yield REDIRECT_CONCLUIDO;
            }
            case RegisterAdolescentService.Outcome.EmailConflict ignored -> {
                session.removeAttribute(SESSION_KEY);
                yield "redirect:/cadastro/adolescente/escolher-metodo?error=account_exists";
            }
            case RegisterAdolescentService.Outcome.Registered ignored ->
                throw new IllegalStateException(
                        "registerFromGoogle nao retorna Registered — esse outcome e exclusivo do fluxo email/senha");
        };
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /**
     * Reutiliza a mesma logica do {@code AdolescentRegistrationController}:
     * bloqueio so quando o unico FieldError do BindingResult e
     * {@code @EligibleAge}. Erros compostos preservam o caminho do form,
     * nao revelando a regra interna de idade (CA-4 US-005).
     *
     * <p>Esta task duplica deliberadamente a logica em vez de extrair pra
     * util — REF futuro (Codificador anotou no resumo).</p>
     */
    private String detectAgeBlockVariant(BindingResult br) {
        if (br.getFieldErrorCount() != 1) {
            return null;
        }
        FieldError birthError = br.getFieldError("birthDate");
        if (birthError == null) {
            return null;
        }
        String[] codes = birthError.getCodes();
        if (codes == null) {
            return null;
        }
        boolean isEligibleAgeViolation = false;
        for (String c : codes) {
            if (c != null && c.startsWith("EligibleAge")) {
                isEligibleAgeViolation = true;
                break;
            }
        }
        if (!isEligibleAgeViolation) {
            return null;
        }
        Object rejected = birthError.getRejectedValue();
        if (rejected instanceof LocalDate birthDate) {
            int age = Period.between(birthDate, LocalDate.now(clock)).getYears();
            return age < 13 ? "under-13" : "over-17";
        }
        return "under-13";
    }
}
