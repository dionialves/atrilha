package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.auth.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class PostLoginRedirectController {

    @GetMapping("/trilha")
    String trilha(@AuthenticationPrincipal AuthenticatedPrincipal principal, Model model) {
        model.addAttribute("displayName", principal.displayName());
        return "trilha/placeholder";
    }

    @GetMapping("/painel")
    String painel() {
        return "painel/placeholder";
    }

    @GetMapping("/vincular")
    String vincular(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        if (principal.hasGuardianLink()) {
            return "redirect:/painel";
        }
        return "vinculacao/inserir-codigo-placeholder";
    }
}
