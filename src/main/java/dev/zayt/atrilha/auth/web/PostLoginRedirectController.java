package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.auth.login.AtrilhaUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class PostLoginRedirectController {

    @GetMapping("/trilha")
    String trilha(@AuthenticationPrincipal AtrilhaUserDetails principal, Model model) {
        model.addAttribute("displayName", principal.getAccount().displayName());
        return "trilha/placeholder";
    }

    @GetMapping("/painel")
    String painel() {
        return "painel/placeholder";
    }

    @GetMapping("/vincular")
    String vincular(@AuthenticationPrincipal AtrilhaUserDetails principal) {
        if (principal.hasGuardianLink()) {
            return "redirect:/painel";
        }
        return "vinculacao/inserir-codigo-placeholder";
    }
}
