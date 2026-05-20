package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.SessionAuthenticator;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

/**
 * Controller do cadastro de adolescente (US-001).
 *
 * <p>Fluxo:
 * <ul>
 *   <li>{@code GET /cadastro/adolescente} → renderiza o form.</li>
 *   <li>{@code POST /cadastro/adolescente} → valida (Jakarta), distingue entre:
 *     <ul>
 *       <li>erro de idade ({@code @EligibleAge}) → renderiza
 *           {@code cadastro/adolescente_bloqueado} com variante.</li>
 *       <li>outros erros de validação → renderiza o form mantendo valores.</li>
 *       <li>e-mail conflitante → renderiza o form com erro inline em
 *           {@code email}.</li>
 *       <li>sucesso → autentica sessão e redireciona para
 *           {@code /verificar-email}.</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 */
@Controller
@RequestMapping("/cadastro/adolescente")
class AdolescentRegistrationController {

    private static final String FORM_VIEW = "cadastro/adolescente";
    private static final String BLOCKED_VIEW = "cadastro/adolescente_bloqueado";
    private static final String VERIFY_EMAIL_REDIRECT = "redirect:/verificar-email";

    private final RegisterAdolescentService service;
    private final SessionAuthenticator sessionAuthenticator;
    private final Clock clock;

    AdolescentRegistrationController(RegisterAdolescentService service,
                                     SessionAuthenticator sessionAuthenticator,
                                     Clock clock) {
        this.service = service;
        this.sessionAuthenticator = sessionAuthenticator;
        this.clock = clock;
    }

    @GetMapping
    String renderForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterAdolescentForm());
        }
        return FORM_VIEW;
    }

    @PostMapping
    String submit(@Valid @ModelAttribute("form") RegisterAdolescentForm form,
                  BindingResult bindingResult,
                  @RequestParam(value = "photo", required = false) MultipartFile photo,
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

        RegisterAdolescentService.Outcome outcome = service.register(form.toRequest(), photo);

        return switch (outcome) {
            case RegisterAdolescentService.Outcome.Registered r -> {
                sessionAuthenticator.authenticate(request, response, r.accountId(), AccountRole.TEEN);
                yield VERIFY_EMAIL_REDIRECT;
            }
            case RegisterAdolescentService.Outcome.EmailConflict ignored -> {
                bindingResult.rejectValue("email", "email.duplicate",
                        "Esse e-mail já tem conta. Quer entrar?");
                yield FORM_VIEW;
            }
            case RegisterAdolescentService.Outcome.GoogleRegistered ignored ->
                throw new IllegalStateException(
                        "register(...) (US-001) nunca devolve GoogleRegistered — esse outcome e exclusivo de registerFromGoogle (US-002)");
        };
    }

    /**
     * Verifica se o {@link BindingResult} contém <strong>exclusivamente</strong>
     * uma violação de {@code @EligibleAge} no campo {@code birthDate} e, em caso
     * positivo, deriva a variante de bloqueio a partir da idade calculada (não
     * da mensagem traduzida, que mudaria entre locales).
     *
     * <p>A precondição "único FieldError do BindingResult" é deliberada: a
     * tela de bloqueio só deve aparecer quando o problema é exclusivamente a
     * faixa etária. Erros compostos (idade fora + e-mail inválido, idade fora
     * + senha curta, etc.) caem no form normal, preservando o que o usuário
     * já preencheu (CA-3 e CA-4 da US-001) e sem revelar indiretamente a
     * regra interna de idade (CA-4 da US-005).</p>
     *
     * @return {@code "under-13"} se nasceu há menos de 13 anos,
     *         {@code "over-17"} se tem 18+, ou {@code null} se a falha não
     *         é exclusivamente uma violação de idade (delegada ao caminho
     *         de form).
     */
    private String detectAgeBlockVariant(BindingResult br) {
        // Bloqueio só aplica quando o ÚNICO erro é o de faixa etária.
        // Erros compostos preservam o caminho do form, sem revelar a regra.
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

        // Pega o valor submetido (não convertido) para distinguir entre
        // muito jovem e muito velho. Spring expõe o valor rejeitado via
        // FieldError.getRejectedValue() — pode ser LocalDate, String, ou null.
        Object rejected = birthError.getRejectedValue();
        if (rejected instanceof LocalDate birthDate) {
            int age = Period.between(birthDate, LocalDate.now(clock)).getYears();
            return age < 13 ? "under-13" : "over-17";
        }
        // Fallback raro: sem valor binding, assume under-13 (mais comum em
        // formulário vazio é não preencher data).
        return "under-13";
    }
}
