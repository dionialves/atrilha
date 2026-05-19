package dev.zayt.atrilha.auth;

/**
 * Três casos de violação de faixa etária no cadastro (US-005 CA-1/2/3).
 *
 * <p>Cada constante mapeia para uma chave de {@code messages.properties}.
 * A chave técnica permanece em inglês; o valor é traduzido para pt-BR.</p>
 */
public enum AgeEligibilityViolation {

    /** Adolescente com idade &lt; 13 anos — mensagem M1. */
    TEEN_TOO_YOUNG("validation.age.teen.tooYoung"),

    /** Adolescente com idade &ge; 18 anos — mensagem M2. */
    TEEN_TOO_OLD("validation.age.teen.tooOld"),

    /** Responsável com idade &lt; 18 anos — mensagem M3. */
    GUARDIAN_TOO_YOUNG("validation.age.guardian.tooYoung");

    private final String messageKey;

    AgeEligibilityViolation(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
