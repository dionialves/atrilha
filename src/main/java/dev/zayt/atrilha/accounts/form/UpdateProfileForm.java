package dev.zayt.atrilha.accounts.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Bean de formulário da edição do perfil (US-009).
 *
 * <p>Wrapper mutável para data binding Spring + Thymeleaf. Campos: nickname
 * e birthDate — os únicos editáveis nesta fase. E-mail é readonly na view.</p>
 */
public class UpdateProfileForm {

    @NotBlank
    @Size(min = 3, max = 20)
    private String nickname;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

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
}
