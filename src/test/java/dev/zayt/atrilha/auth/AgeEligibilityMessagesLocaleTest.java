package dev.zayt.atrilha.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Locale-resilience das chaves de {@code messages.properties} (US-005).
 *
 * <p>Cobre o gap apontado pelo Codificador (item 4): forçar
 * {@link Locale#US} em runtime e confirmar que, com
 * {@code fallback-to-system-locale=false}, a resolução ainda funciona
 * para pt-BR e não cai silenciosamente em outro idioma.</p>
 *
 * <p>O comportamento exato esperado de um
 * {@link ResourceBundleMessageSource} com {@code fallbackToSystemLocale=false}
 * e uma chave existindo somente em {@code messages.properties} (sem
 * sufixo de locale) é: resolver para o bundle <em>default</em> independente
 * do Locale solicitado. Esse é justamente o comportamento desejado
 * — pt-BR é o único idioma do produto no MVP.</p>
 */
class AgeEligibilityMessagesLocaleTest {

    private Locale originalDefault;
    private ResourceBundleMessageSource messages;

    @BeforeEach
    void setUp() {
        originalDefault = Locale.getDefault();
        messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
    }

    @AfterEach
    void tearDown() {
        Locale.setDefault(originalDefault);
    }

    @Test
    void messages_underLocaleUs_stillResolveFromDefaultBundle() {
        Locale.setDefault(Locale.US);

        for (AgeEligibilityViolation v : AgeEligibilityViolation.values()) {
            String resolved = messages.getMessage(v.messageKey(), null, Locale.US);
            assertThat(resolved)
                    .as("mensagem para %s sob Locale.US", v)
                    .isNotBlank()
                    .isNotEqualTo(v.messageKey());
            // Conteúdo pt-BR — pelo menos um caractere acentuado ou substring portuguesa.
            assertThat(resolved.toLowerCase(Locale.ROOT))
                    .containsAnyOf("você", "é", "ã", "ç");
        }
    }

    @Test
    void messages_underLocaleJapan_stillResolveFromDefaultBundle() {
        // Locale totalmente alienígena — confirma que a ausência de
        // messages_ja.properties não impede resolução do default bundle.
        Locale.setDefault(Locale.JAPAN);

        String resolved = messages.getMessage(
                AgeEligibilityViolation.TEEN_TOO_OLD.messageKey(),
                null,
                Locale.JAPAN);

        assertThat(resolved).isNotBlank();
        assertThat(resolved.toLowerCase(Locale.ROOT)).contains("respons");
    }

    @Test
    void messages_fallbackDisabled_unknownLocaleStillResolvesViaDefaultBundle() {
        // Comportamento documentado do ResourceBundleMessageSource quando
        // fallbackToSystemLocale=false: chave continua resolvendo via
        // bundle base (messages.properties sem sufixo). Se alguém remexer
        // na configuração e reativar fallback para o sistema, esse teste
        // continua passando — porque não falha. O que ele garante é o
        // CONTRATO de não devolver a chave bruta.
        String resolved = messages.getMessage(
                AgeEligibilityViolation.GUARDIAN_TOO_YOUNG.messageKey(),
                null,
                Locale.forLanguageTag("xx-YY")); // locale inexistente

        assertThat(resolved)
                .isNotBlank()
                .isNotEqualTo(AgeEligibilityViolation.GUARDIAN_TOO_YOUNG.messageKey());
    }

    @Test
    void messages_defaultFallbackKey_validationAgeInvalid_resolves() {
        // A anotação @EligibleAge declara message default
        // "{validation.age.invalid}". Se a chave sumir, qualquer override
        // pelo validator continua funcionando, mas o fallback default
        // ficaria órfão. Teste de regressão para o contrato da anotação.
        String resolved = messages.getMessage(
                "validation.age.invalid",
                null,
                Locale.forLanguageTag("pt-BR"));
        assertThat(resolved)
                .isNotBlank()
                .isNotEqualTo("validation.age.invalid");
    }

    @Test
    void messages_unknownKeyUnderForeignLocale_stillThrows() {
        // Garante que a configuração de locale não mascara erros de chave
        // ausente (i.e. um typo no key permanece detectável).
        assertThatThrownBy(() -> messages.getMessage(
                "validation.age.nonexistent.key",
                null,
                Locale.US))
                .isInstanceOf(NoSuchMessageException.class);
    }
}
