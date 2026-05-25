package dev.zayt.atrilha.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Porta de entrada do fluxo de cadastro (US-001 CA-1).
 *
 * <p>Apresenta os dois caminhos — "Sou adolescente" e "Sou responsável" —
 * antes do usuário informar e-mail/senha.</p>
 */
@Controller
class StartFlowController {

    @GetMapping("/comecar")
    String start() {
        return "comecar";
    }
}
