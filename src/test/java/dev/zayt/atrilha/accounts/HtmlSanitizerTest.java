package dev.zayt.atrilha.accounts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wrapper Jsoup que limpa texto livre antes de persistir (US-001, DoD §2.4).
 *
 * <p>O apelido é o caso pivot: usuário pode digitar HTML; servidor remove
 * antes de gravar e/ou renderizar. Política {@code Safelist.none()}: nenhuma
 * tag sobrevive. Texto puro passa intacto, exceto por trim de borda.</p>
 */
class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void stripsAllHtmlTagsFromText() {
        assertThat(sanitizer.clean("<script>alert(1)</script>ju"))
                .isEqualTo("ju");
    }

    @Test
    void stripsNestedTagsAndAttributes() {
        assertThat(sanitizer.clean("<b onclick=\"x\">o<i>la</i></b>"))
                .isEqualTo("ola");
    }

    @Test
    void preservesPlainText() {
        assertThat(sanitizer.clean("ju")).isEqualTo("ju");
    }

    @Test
    void preservesUnicodeAndAccents() {
        assertThat(sanitizer.clean("Júlia")).isEqualTo("Júlia");
    }

    @Test
    void trimsLeadingAndTrailingWhitespace() {
        assertThat(sanitizer.clean("  ju  ")).isEqualTo("ju");
    }

    @Test
    void returnsNullWhenInputIsNull() {
        assertThat(sanitizer.clean(null)).isNull();
    }

    @Test
    void emptyStringRemainsEmpty() {
        assertThat(sanitizer.clean("")).isEqualTo("");
    }
}
