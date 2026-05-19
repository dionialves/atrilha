package dev.zayt.atrilha.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Resolução das chaves M1/M2/M3 em pt-BR a partir de
 * {@code src/main/resources/messages.properties} — Bloco C da Issue #36.
 *
 * <p>Usa {@link ResourceBundleMessageSource} direto (sem
 * {@code @SpringBootTest}) para ser leve e validar o arquivo de mensagens
 * de forma independente do contexto Spring.</p>
 */
class AgeEligibilityMessagesTest {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private ResourceBundleMessageSource messages;

    @BeforeEach
    void setUp() {
        messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
    }

    // ---- C1 ----
    @Test
    void messages_teenTooYoung_resolvesInPtBr() {
        String key = AgeEligibilityViolation.TEEN_TOO_YOUNG.messageKey();
        String value = messages.getMessage(key, null, PT_BR);
        assertThat(value).isNotBlank();
        assertThat(value).isNotEqualTo(key); // não voltou a chave literal
    }

    // ---- C2 ----
    @Test
    void messages_teenTooOld_resolvesAndMentionsResponsavelPath() {
        String key = AgeEligibilityViolation.TEEN_TOO_OLD.messageKey();
        String value = messages.getMessage(key, null, PT_BR);
        assertThat(value).isNotBlank();
        assertThat(value.toLowerCase(PT_BR)).contains("respons");
    }

    // ---- C3 ----
    @Test
    void messages_guardianTooYoung_resolvesAndMentionsAdolescentePath() {
        String key = AgeEligibilityViolation.GUARDIAN_TOO_YOUNG.messageKey();
        String value = messages.getMessage(key, null, PT_BR);
        assertThat(value).isNotBlank();
        assertThat(value.toLowerCase(PT_BR)).contains("adolescente");
    }

    // ---- C4 ----
    @Test
    void messages_noneMentionForbiddenTerms() {
        List<String> forbidden = List.of(
                "criança", "menor de idade", "criancinha", "pequeno", "infantil");

        for (AgeEligibilityViolation v : AgeEligibilityViolation.values()) {
            String value = messages.getMessage(v.messageKey(), null, PT_BR).toLowerCase(PT_BR);
            for (String term : forbidden) {
                assertThat(value)
                        .as("mensagem '%s' não deve conter termo proibido '%s'", v, term)
                        .doesNotContain(term);
            }
        }
    }

    // ---- C5 ----
    @Test
    void messages_encodingIsUtf8() {
        // Confirma que ao menos um caractere multibyte UTF-8 está presente
        // e legível em pelo menos uma das três mensagens (acentos pt-BR).
        boolean anyMultibyteOk = false;
        for (AgeEligibilityViolation v : AgeEligibilityViolation.values()) {
            String value = messages.getMessage(v.messageKey(), null, PT_BR);
            // qualquer caractere fora do ASCII puro indica encoding correto
            if (value.chars().anyMatch(c -> c > 127)) {
                anyMultibyteOk = true;
                // não pode aparecer o "?" de fallback nem mojibake típico
                assertThat(value).doesNotContain("Ã©", "Ã¡", "Ã­", "Ã³", "Ãº", "Ã£", "Ã§");
            }
        }
        assertThat(anyMultibyteOk).as("Esperava ao menos um caractere acentuado pt-BR").isTrue();
    }

    @Test
    void messages_unknownKey_throws() {
        assertThatThrownBy(() -> messages.getMessage("validation.age.does.not.exist", null, PT_BR))
                .isInstanceOf(NoSuchMessageException.class);
    }
}
