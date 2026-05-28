package dev.zayt.atrilha.auth.domain;

/**
 * Resultado da verificação de token de recuperação de senha (US-008-a).
 *
 * <p>{@code EXPIRED_OR_INVALID} unifica "token inexistente" e "token expirado"
 * propositadamente: a UX spec §5.3 exige a mesma tela para os dois casos
 * (privacidade — não revelar se o token existiu). Espelha {@link VerificationResult}.</p>
 */
public enum PasswordResetResult {

    /** Token válido, consumido com sucesso. */
    SUCCESS,

    /** Token já foi consumido anteriormente (used_at != null). */
    ALREADY_USED,

    /** Token inexistente ou expirado — unificado para não vazar informação de existência (privacidade). */
    EXPIRED_OR_INVALID
}
