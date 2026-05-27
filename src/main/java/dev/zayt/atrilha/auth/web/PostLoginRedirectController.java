package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import dev.zayt.atrilha.accounts.repository.AdolescentProfileRepository;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import dev.zayt.atrilha.auth.domain.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class PostLoginRedirectController {

    private final AdolescentProfileRepository profileRepo;

    PostLoginRedirectController(AdolescentProfileRepository profileRepo) {
        this.profileRepo = profileRepo;
    }

    @GetMapping("/trilha")
    String trilha(Authentication authentication, Model model) {
        if (authentication == null) {
            model.addAttribute("displayName", "Amigo");
            return "trilha/placeholder";
        }

        Object principal = authentication.getPrincipal();

        String displayName;
        if (principal instanceof AuthenticatedAccount acc) {
            // Fluxo de cadastro: principal é AuthenticatedAccount (UUID + role).
            // Buscar o nickname do perfil do adolescente.
            displayName = profileRepo
                .findByAccountId(acc.id())
                .map(AdolescentProfile::getNickname)
                .orElse(acc.id().toString().substring(0, 8));
        } else if (principal instanceof AuthenticatedPrincipal ap) {
            // Fluxo de form login: principal é AtrilhaUserDetails.
            displayName = ap.displayName();
        } else {
            // Fallback defensivo (não deveria acontecer com Spring Security ativo).
            displayName = "Amigo";
        }

        model.addAttribute("displayName", displayName);
        return "trilha/placeholder";
    }

    @GetMapping("/painel")
    String painel() {
        return "painel/placeholder";
    }

    @GetMapping("/vincular")
    String vincular(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedAccount acc) {
            // Recém-cadastrado via SessionAuthenticator — ainda não tem
            // AuthenticatedPrincipal resolvido. Permite acesso sem hasGuardianLink().
            return "vinculacao/inserir-codigo-placeholder";
        }

        if (principal instanceof AuthenticatedPrincipal ap) {
            if (ap.hasGuardianLink()) {
                return "redirect:/painel";
            }
        }

        // Fallback: mostra a tela de vinculação.
        return "vinculacao/inserir-codigo-placeholder";
    }
}
