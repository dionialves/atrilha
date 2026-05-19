package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.EligibleAge;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Bean de formulário do cadastro de adolescente — wrapper "form-friendly"
 * de {@link RegisterAdolescentRequest}.
 *
 * <p>Usamos uma classe mutável separada do record do request porque o
 * data binding do Spring + Thymeleaf precisa de setters/getters para
 * repopular o form em caso de erro (o record é imutável e o binder pode
 * falhar silenciosamente em campos não-nulos).</p>
 */
public class RegisterAdolescentForm {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Size(min = 3, max = 20)
    private String nickname;

    @NotNull
    @EligibleAge(role = AccountRole.TEEN)
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    RegisterAdolescentRequest toRequest() {
        return new RegisterAdolescentRequest(email, password, nickname, birthDate);
    }
}
