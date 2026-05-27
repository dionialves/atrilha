package dev.zayt.atrilha.accounts.web;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.accounts.domain.RegisterGuardianRequest;
import dev.zayt.atrilha.accounts.validation.EligibleAge;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Bean de formulário do cadastro de responsável — wrapper "form-friendly"
 * de {@link RegisterGuardianRequest}.
 *
 * <p>Usamos uma classe mutável separada do record do request porque o
 * data binding do Spring + Thymeleaf precisa de setters/getters para
 * repopular o form em caso de erro (o record é imutável e o binder pode
 * falhar silenciosamente em campos não-nulos).</p>
 */
public class RegisterGuardianForm {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Size(min = 3, max = 100)
    private String fullName;

    @NotNull
    @EligibleAge(role = AccountRole.GUARDIAN)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    RegisterGuardianRequest toRequest() {
        return new RegisterGuardianRequest(email, password, fullName, birthDate);
    }
}
