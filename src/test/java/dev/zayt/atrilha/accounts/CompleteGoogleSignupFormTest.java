package dev.zayt.atrilha.accounts;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.EligibleAge;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validacao do {@link CompleteGoogleSignupForm} (US-002).
 *
 * <p>Como o {@link dev.zayt.atrilha.auth.AgeEligibilityChecker} e o
 * validador {@code EligibleAge} sao package-private no modulo {@code auth},
 * os testes aqui inspecionam a anotacao via reflexao para garantir que
 * o contrato declarado pelo plano da Issue #37 esta presente. A logica de
 * idade em si ja e coberta exaustivamente pela suite da US-005
 * (EligibleAgeValidatorTest, AgeEligibilityCheckerTest, etc.) — esta
 * classe garante apenas que CompleteGoogleSignupForm participa do mesmo
 * contrato (mesma anotacao, mesmo role TEEN).</p>
 *
 * <p>Os limites numericos (Size, NotBlank, NotNull) sao validados aqui
 * tambem por metadado da anotacao — assim regressao do tipo "alguem trocou
 * @Size(min=3,max=20) por @Size(max=30)" e detectada.</p>
 */
class CompleteGoogleSignupFormTest {

    // ---- Apelido 3..20 ----

    @Test
    void validacaoApelido3a20DeclaradoNoCampo() throws Exception {
        Field f = CompleteGoogleSignupForm.class.getDeclaredField("nickname");
        Size size = f.getAnnotation(Size.class);
        assertThat(size).as("@Size(min=3, max=20) ausente em nickname").isNotNull();
        assertThat(size.min()).isEqualTo(3);
        assertThat(size.max()).isEqualTo(20);
    }

    @Test
    void apelidoNotBlank() throws Exception {
        Field f = CompleteGoogleSignupForm.class.getDeclaredField("nickname");
        assertThat(f.getAnnotation(NotBlank.class))
                .as("@NotBlank ausente em nickname").isNotNull();
    }

    // ---- birthDate obrigatorio ----

    @Test
    void validacaoBirthdateObrigatoria() throws Exception {
        Field f = CompleteGoogleSignupForm.class.getDeclaredField("birthDate");
        assertThat(f.getAnnotation(NotNull.class))
                .as("@NotNull ausente em birthDate").isNotNull();
    }

    // ---- birthDate idade teen ----

    @Test
    void validacaoBirthdateIdadeTeenAnotada() throws Exception {
        Field f = CompleteGoogleSignupForm.class.getDeclaredField("birthDate");
        EligibleAge anno = f.getAnnotation(EligibleAge.class);
        assertThat(anno).as("@EligibleAge ausente em birthDate").isNotNull();
        assertThat(anno.role()).isEqualTo(AccountRole.TEEN);
    }

    @Test
    void birthDateTipoLocalDate() throws Exception {
        Field f = CompleteGoogleSignupForm.class.getDeclaredField("birthDate");
        assertThat(f.getType()).isEqualTo(LocalDate.class);
    }

    // ---- PhotoSource ----

    @Test
    void photoSourceEnumPossuiTresOpcoes() {
        assertThat(CompleteGoogleSignupForm.PhotoSource.values())
                .containsExactlyInAnyOrder(
                        CompleteGoogleSignupForm.PhotoSource.GOOGLE,
                        CompleteGoogleSignupForm.PhotoSource.UPLOAD,
                        CompleteGoogleSignupForm.PhotoSource.NONE);
    }

    @Test
    void photoSourceDefaultEhNone() {
        CompleteGoogleSignupForm form = new CompleteGoogleSignupForm();
        assertThat(form.getPhotoSource())
                .as("default deve evitar NPE no controller quando radio nao chega")
                .isEqualTo(CompleteGoogleSignupForm.PhotoSource.NONE);
    }

    @Test
    void photoSourceNotNullDeclarado() throws Exception {
        Field f = CompleteGoogleSignupForm.class.getDeclaredField("photoSource");
        assertThat(f.getAnnotation(NotNull.class))
                .as("@NotNull em photoSource garante 400 quando ausente")
                .isNotNull();
    }

    // ---- Getters/setters basicos para data binding Spring MVC ----

    @Test
    void gettersSettersFuncionam() {
        CompleteGoogleSignupForm form = new CompleteGoogleSignupForm();
        form.setNickname("kira");
        form.setBirthDate(LocalDate.of(2010, 5, 1));
        form.setPhotoSource(CompleteGoogleSignupForm.PhotoSource.GOOGLE);
        assertThat(form.getNickname()).isEqualTo("kira");
        assertThat(form.getBirthDate()).isEqualTo(LocalDate.of(2010, 5, 1));
        assertThat(form.getPhotoSource()).isEqualTo(CompleteGoogleSignupForm.PhotoSource.GOOGLE);
    }
}
