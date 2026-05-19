package dev.zayt.atrilha.accounts;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Stub temporário para o caminho de responsável (US-003) — evita 404 ao
 * clicar no card "Sou responsável" antes do Sprint 4 quando essa US
 * aparecerá.
 *
 * <p>Quando a US-003 for implementada, este controller é removido (ou
 * substituído pelo controller real de cadastro do responsável).</p>
 */
@Controller
class GuardianRegistrationStubController {

    @GetMapping("/cadastro/responsavel")
    String comingSoon() {
        return "cadastro/responsavel_em_breve";
    }
}
