package dev.zayt.atrilha.accounts.domain;

/**
 * Papel de uma conta no atrilha (RF-E1-03).
 *
 * <p>Distinção fundamental do cadastro: adolescente (13–17) ou responsável
 * (18+). Usada para determinar a faixa etária válida.</p>
 *
 * <p>Mantida {@code public} porque é referenciada por anotações em DTOs
 * de outros pacotes.</p>
 */
public enum AccountRole {
    TEEN,
    GUARDIAN
}
