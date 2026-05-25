package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.AuthenticatedPrincipal;
import dev.zayt.atrilha.auth.login.AtrilhaOAuth2User;
import dev.zayt.atrilha.auth.login.PostLoginDestination;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class LoginController {

    private static final String VIEW_LOGIN = "auth/login";

    @GetMapping("/login")
    String renderLogin(HttpServletRequest request, Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof AuthenticatedPrincipal principal
                && !(principal instanceof AtrilhaOAuth2User u && u.isPendingSignup())) {
            return "redirect:" + resolveDestinationFrom(principal).path();
        }

        // Spring Security redireciona para /login?error e /login?logout sem
        // "=" no parâmetro. @RequestParam.getParameter() retorna "" no Tomcat
        // mas null no MockMvc — checamos presença no parameter map p/ funcionar
        // nos dois ambientes.
        var params = request.getParameterMap();
        if (params.containsKey("blocked")) {
            model.addAttribute("errorState", "rate-limited");
        } else if (params.containsKey("error")) {
            model.addAttribute("errorState", "bad-credentials");
        }
        if (params.containsKey("logout")) {
            model.addAttribute("infoState", "logged-out");
        }

        return VIEW_LOGIN;
    }

    private PostLoginDestination resolveDestinationFrom(AuthenticatedPrincipal principal) {
        if (principal.role() == AccountRole.TEEN) {
            return PostLoginDestination.TRILHA;
        }
        return principal.hasGuardianLink()
                ? PostLoginDestination.PAINEL
                : PostLoginDestination.VINCULAR;
    }
}
