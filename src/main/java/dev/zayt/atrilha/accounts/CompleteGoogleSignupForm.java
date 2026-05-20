package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.EligibleAge;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Bean de formulario da etapa de complementacao apos OAuth Google (US-002).
 *
 * <p>Spring MVC + Thymeleaf precisam de getters/setters para o
 * data binding repopular o form em caso de erro — por isso classe mutavel
 * (mesmo padrao de {@link RegisterAdolescentForm}).</p>
 *
 * <p>E-mail e foto Google ja chegam via {@code PendingGoogleSignup} na sessao
 * — nao sao campos do form. O upload manual de foto chega como
 * {@code MultipartFile} separado (igual ao fluxo US-001), apenas quando
 * {@link PhotoSource#UPLOAD} esta selecionado.</p>
 */
public class CompleteGoogleSignupForm {

    @NotBlank
    @Size(min = 3, max = 20)
    private String nickname;

    @NotNull
    @EligibleAge(role = AccountRole.TEEN)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @NotNull
    private PhotoSource photoSource = PhotoSource.NONE;

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

    public PhotoSource getPhotoSource() {
        return photoSource;
    }

    public void setPhotoSource(PhotoSource photoSource) {
        this.photoSource = photoSource;
    }

    /**
     * Origem da foto de avatar no fluxo Google (US-002).
     */
    public enum PhotoSource {
        /** Usar a URL retornada pelo Google (claim {@code picture}). */
        GOOGLE,
        /** Upload manual via {@code MultipartFile} (passa por AvatarStorage). */
        UPLOAD,
        /** Sem foto — avatar_url permanece null (fallback inicial do apelido). */
        NONE
    }
}
