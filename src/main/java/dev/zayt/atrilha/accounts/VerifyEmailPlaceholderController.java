package dev.zayt.atrilha.accounts;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Placeholder estático de {@code /verificar-email} (US-001 CA-5).
 *
 * <p>A US-006 (próxima do Sprint 3) substitui este controller pelo fluxo
 * real de verificação por e-mail. Por ora, apenas confirma ao usuário que
 * o cadastro aconteceu e que ele já está logado.</p>
 *
 * <p>Esta rota é a única exigindo sessão autenticada nesta US (configurada
 * em {@code SecurityConfig}); por isso o controller assume que o usuário
 * já está logado quando chega aqui.</p>
 */
@Controller
class VerifyEmailPlaceholderController {

    @GetMapping("/verificar-email")
    String renderPlaceholder() {
        return "verificar-email";
    }
}
