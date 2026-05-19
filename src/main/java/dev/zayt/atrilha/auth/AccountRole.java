package dev.zayt.atrilha.auth;

/**
 * Papel de uma conta no atrilha (RF-E1-03).
 *
 * <p>Distinção fundamental do cadastro: adolescente (13–17) ou responsável
 * (18+). Usada por {@link EligibleAge} para determinar a faixa etária
 * válida.</p>
 *
 * <p>Mantida {@code public} porque será referenciada por anotações em DTOs
 * de outros pacotes a partir da US-001.</p>
 */
public enum AccountRole {
    TEEN,
    GUARDIAN
}
