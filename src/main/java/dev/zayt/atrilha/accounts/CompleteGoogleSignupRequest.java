package dev.zayt.atrilha.accounts;

import java.time.LocalDate;

/**
 * DTO imutavel passado ao {@link RegisterAdolescentService#registerFromGoogle}
 * (US-002). Mantido package-private — apenas o controller do modulo
 * {@code accounts} chama essa API.
 *
 * <p>Nao carrega e-mail nem foto Google — esses vivem no
 * {@link dev.zayt.atrilha.auth.PendingGoogleSignup} (sessao). O upload manual
 * de foto chega como {@code MultipartFile} separado para evitar serializar
 * binarios em DTO de teste.</p>
 */
record CompleteGoogleSignupRequest(
        String nickname,
        LocalDate birthDate,
        CompleteGoogleSignupForm.PhotoSource photoSource) {
}
