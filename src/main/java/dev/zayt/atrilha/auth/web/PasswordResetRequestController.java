package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.repository.AccountReader;
import dev.zayt.atrilha.auth.verification.PasswordResetSender;
import dev.zayt.atrilha.auth.verification.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Controla o fluxo de solicitação de redefinição de senha (metade frontal).
 * Expõe GET/POST /esqueci-senha para usuários não autenticados.
 * Resposta de sucesso é idêntica independente de o e-mail estar cadastrado (anti-enumeration).
 */
@Controller
@RequestMapping("/esqueci-senha")
class PasswordResetRequestController {

    private static final String VIEW_FORM = "auth/esqueci-senha";
    private static final String REDIRECT_SUCCESS = "redirect:/esqueci-senha?enviado=1";

    private final AccountReader accountReader;
    private final PasswordResetService passwordResetService;
    private final PasswordResetSender passwordResetSender;

    PasswordResetRequestController(AccountReader accountReader,
                                    PasswordResetService passwordResetService,
                                    PasswordResetSender passwordResetSender) {
        this.accountReader = accountReader;
        this.passwordResetService = passwordResetService;
        this.passwordResetSender = passwordResetSender;
    }

    @GetMapping
    String showForm(@ModelAttribute("request") ForgotPasswordRequest request) {
        return VIEW_FORM;
    }

    @PostMapping
    String requestReset(@Valid @ModelAttribute("request") ForgotPasswordRequest request,
                        BindingResult bindingResult,
                        RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            return VIEW_FORM;
        }

        Optional<Account> account = accountReader.findByEmailIgnoreCase(request.getEmail());
        if (account.isPresent()) {
            var token = passwordResetService.issueToken(account.get());
            // Account não expõe nickname — passa string vazia.
            // O sender lida graceful com nickname vazio (JavaMail usa "Amigo" como fallback).
            passwordResetSender.sendReset(request.getEmail(), "", token);
        }
        // Anti-oracle: mesmo redirect independente de existir ou não.

        ra.addFlashAttribute("sent", true);
        return REDIRECT_SUCCESS;
    }

    /**
     * DTO para o formulário de solicitação de reset.
     */
    static class ForgotPasswordRequest {

        @NotBlank(message = "{password-reset.form.errors.email.required}")
        @Email(message = "{password-reset.form.errors.email.invalid}")
        private String email;

        ForgotPasswordRequest() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
