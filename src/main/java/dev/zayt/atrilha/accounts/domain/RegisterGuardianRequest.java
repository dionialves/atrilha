package dev.zayt.atrilha.accounts.domain;

import dev.zayt.atrilha.accounts.validation.EligibleAge;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de entrada do cadastro de responsável (US-003).
 *
 * <p>Restrições Jakarta Validation:
 * <ul>
 *   <li>{@code email} — não-vazio, formato válido, ≤ 255 chars.</li>
 *   <li>{@code password} — 8–72 chars (BCrypt trunca em 72).</li>
 *   <li>{@code fullName} — 3–100 chars (espelha o comprimento do DB).</li>
 *   <li>{@code birthDate} — não-null e idade ≥ 18
 *       ({@link EligibleAge} com {@code role = GUARDIAN}).</li>
 * </ul>
 * </p>
 */
public record RegisterGuardianRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(min = 3, max = 100) String fullName,
        @NotNull @EligibleAge(role = AccountRole.GUARDIAN) LocalDate birthDate
) {
}
