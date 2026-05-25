package dev.zayt.atrilha.auth.verification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um endpoint (ou um controller inteiro) como exigindo que a conta
 * autenticada tenha o e-mail verificado (US-006 critério 5).
 *
 * <p>Aplicação: {@code @RequiresVerifiedEmail} sobre um método {@code @GetMapping}
 * /{@code @PostMapping} (ou em cima da classe do controller, para cobrir todos
 * os métodos). O {@link RequiresVerifiedEmailInterceptor} faz o enforcement:
 * <ul>
 *   <li>Usuário com {@code email_verified_at == null} → redirect 302 para
 *       {@code /verificar-email} em GETs; 403 em POSTs.</li>
 *   <li>Usuário verificado → segue normalmente.</li>
 *   <li>Anonymous → segue (autorização de login fica com Spring Security).</li>
 * </ul>
 * </p>
 *
 * <p>Pública porque consumidores (US-012/US-014 — vinculação) vivem em
 * pacotes diferentes. Nesta US a anotação <strong>não</strong> é aplicada em
 * nenhum endpoint real — apenas plantamos o ponto de extensão.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresVerifiedEmail {
}
