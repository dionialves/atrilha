package dev.zayt.atrilha.accounts.web;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.accounts.service.RegisterGuardianService;
import dev.zayt.atrilha.auth.session.SessionAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

/**
 * Controller do cadastro de responsável (US-003).
 *
 * <p>Fluxo:
 * <ul>
 *   <li>{@code GET /cadastro/responsavel} → renderiza o form.</li>
 *   <li>{@code POST /cadastro/responsavel} → valida (Jakarta), distingue entre:
 *     <ul>
 *       <li>erro de idade ({@code @EligibleAge}) → renderiza
 *           {@code cadastro/responsavel_bloqueado} com variante.</li>
 *       <li>outros erros de validação → renderiza o form mantendo valores.</li>
 *       <li>e-mail conflitante → renderiza o form com erro inline em
 *           {@code email}.</li>
 *       <li>sucesso → autentica sessão e redireciona para
 *           {@code /vincular}.</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/cadastro/responsavel")
class GuardianRegistrationController {

    private static final String FORM_VIEW = "cadastro/responsavel";
    private static final String BLOCKED_VIEW = "cadastro/responsavel_bloqueado";
    private static final String VINCULAR_REDIRECT = "redirect:/vincular";
    private static final String ESCOLHER_METODO_VIEW = "cadastro/responsavel_escolher_metodo";

    private final RegisterGuardianService service;
    private final SessionAuthenticator sessionAuthenticator;
    private final Clock clock;

    GuardianRegistrationController(RegisterGuardianService service,
                                   SessionAuthenticator sessionAuthenticator,
                                   Clock clock) {
        this.service = service;
        this.sessionAuthenticator = sessionAuthenticator;
        this.clock = clock;
    }

    @GetMapping("/escolher-metodo")
    String renderEscolherMetodo() {
        return ESCOLHER_METODO_VIEW;
    }

    @GetMapping
    String renderForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterGuardianForm());
        }
        return FORM_VIEW;
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("form") RegisterGuardianForm form,
                  BindingResult bindingResult,
                  Model model,
                  HttpServletRequest request,
                  HttpServletResponse response) {

        if (bindingResult.hasErrors()) {
            // Idade fora da faixa → tela dedicada de bloqueio.
            String ageBlockVariant = detectAgeBlockVariant(bindingResult);
            if (ageBlockVariant != null) {
                model.addAttribute("variant", ageBlockVariant);
                return BLOCKED_VIEW;
            }
            // Demais erros: form com mensagens inline.
            return FORM_VIEW;
        }

        RegisterGuardianService.Outcome outcome = service.register(form.toRequest());

        return switch (outcome) {
            case RegisterGuardianService.Outcome.Registered r -> {
                sessionAuthenticator.authenticate(request, response, r.accountId(), AccountRole.GUARDIAN);
                yield VINCULAR_REDIRECT;
            }
            case RegisterGuardianService.Outcome.EmailConflict ignored -> {
                bindingResult.rejectValue("email", "email.duplicate",
                        "Esse e-mail já tem conta. Quer entrar?");
                yield FORM_VIEW;
            }
        };
    }

    /**
     * Verifica se o {@link BindingResult} contém <strong>exclusivamente</strong>
     * uma violação de {@code @EligibleAge} no campo {@code birthDate} e, em caso
     * positivo, deriva a variante de bloqueio a partir da idade calculada.
     *
     * <p>Para responsável (GUARDIAN), só existe um caso de violação:
     * idade &lt; 18 → variante {@code "under-18"}.</p>
     *
     * @return {@code "under-18"} se tem menos de 18 anos, ou {@code null}
     *         se a falha não é exclusivamente uma violação de idade.
     */
    private String detectAgeBlockVariant(BindingResult br) {
        // Bloqueio só aplica quando o ÚNICO erro é o de faixa etária.
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
            return age < 18 ? "under-18" : null;
        }
        // Fallback: sem valor binding, assume under-18.
        return "under-18";
    }
}
