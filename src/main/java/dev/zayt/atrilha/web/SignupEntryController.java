package dev.zayt.atrilha.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Porta canonica do fluxo de cadastro (US-002 — Tela 1).
 *
 * <p>{@code GET /cadastro} renderiza a tela "Qual caminho comeca pra voce?"
 * com cards para "Sou adolescente" (→ {@code /cadastro/adolescente/escolher-metodo})
 * e "Sou responsavel" (→ {@code /cadastro/responsavel}).</p>
 *
 * <p>{@code GET /cadastro/concluido} renderiza o placeholder de conclusao
 * (Tela 4). O destino real do "proximo passo" e implementado pela US-012
 * (vinculacao responsavel) — esta rota e uma ponte ate la.</p>
 *
 * <p>Convive com {@code /comecar} (StartFlowController da US-001) — a rota
 * legada continua valida (home.html aponta para ela). {@code /cadastro} e a
 * porta canonica daqui pra frente.</p>
 */
@Controller
class SignupEntryController {

    @GetMapping("/cadastro")
    String entry() {
        return "cadastro/escolher-papel";
    }

    @GetMapping("/cadastro/concluido")
    String concluido() {
        return "cadastro/concluido";
    }
}
