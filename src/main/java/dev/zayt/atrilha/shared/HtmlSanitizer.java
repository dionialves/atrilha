package dev.zayt.atrilha.shared;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Wrapper fino sobre Jsoup para limpar texto livre antes de persistência
 * (US-001, DoD §2.4).
 *
 * <p>Política {@link Safelist#none()}: zero tags, zero atributos. O resultado
 * é o conteúdo textual sem nenhuma marcação. Borda de espaços é removida
 * por {@code trim()}; espaços internos são preservados.</p>
 *
 * <p>{@code null} entra, {@code null} sai — facilita uso em fluxos
 * opcionais. Cabe ao chamador validar obrigatoriedade.</p>
 */
@Component
public final class HtmlSanitizer {

    public String clean(String input) {
        if (input == null) {
            return null;
        }
        return Jsoup.clean(input.trim(), Safelist.none());
    }
}
