package dev.zayt.atrilha.accounts.domain;

import dev.zayt.atrilha.accounts.validation.EligibleAge;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de entrada do cadastro de adolescente (US-001).
 *
 * <p>Restrições Jakarta Validation:
 * <ul>
 *   <li>{@code email} — não-vazio, formato válido, ≤ 255 chars.</li>
 *   <li>{@code password} — 8–72 chars (BCrypt trunca em 72).</li>
 *   <li>{@code nickname} — 3–20 chars (também enforced pela constraint do DB).</li>
 *   <li>{@code birthDate} — não-null e idade na faixa 13–17 inclusive
 *       ({@link EligibleAge} reutiliza o validador da US-005).</li>
 * </ul>
 * </p>
 *
 * <p>A foto não faz parte do record — vem como {@code MultipartFile} separado
 * para evitar serialização desnecessária em testes.</p>
 */
public record RegisterAdolescentRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(min = 3, max = 20) String nickname,
        @NotNull @EligibleAge(role = AccountRole.TEEN) LocalDate birthDate
) {
}
