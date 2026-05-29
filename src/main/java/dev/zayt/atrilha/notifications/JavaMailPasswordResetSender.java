package dev.zayt.atrilha.notifications;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import dev.zayt.atrilha.auth.verification.PasswordResetSender;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Envio do e-mail de redefinição de senha via {@link JavaMailSender} (US-008-b).
 *
 * <p>Renderiza dois templates — HTML ({@code email/password-reset.html}) e
 * texto-plano ({@code email/password-reset-plain.txt}) — usando um
 * {@link TemplateEngine} interno, separado do engine do Spring MVC.</p>
 *
 * <p>Justificativa da engine interna: o auto-config do Spring Boot
 * (ThymeleafAutoConfiguration) registra um {@code SpringTemplateEngine}
 * via {@code @ConditionalOnMissingBean(SpringTemplateEngine.class)} —
 * declarar outro bean do mesmo tipo (ou subtipo) desativaria o engine
 * canônico das views. Aqui criamos um {@code TemplateEngine} simples,
 * sem registro como bean Spring, somente para o módulo de e-mails.</p>
 *
 * <p>Logs <strong>nunca</strong> contêm token nem corpo do e-mail
 * (PRD §11.8). Apenas o destinatário e um identificador do tipo de e-mail.</p>
 */
@Component
@Profile("!test")
class JavaMailPasswordResetSender implements PasswordResetSender {

    private static final Logger log = LoggerFactory.getLogger(JavaMailPasswordResetSender.class);

    private static final String SUBJECT = "Redefinição de senha no atrilha";
    private static final String HTML_TEMPLATE = "email/password-reset";
    private static final String TEXT_TEMPLATE = "email/password-reset-plain";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;
    private final String baseUrl;

    JavaMailPasswordResetSender(JavaMailSender mailSender,
                                @Value("${atrilha.mail.from}") String fromAddress,
                                @Value("${atrilha.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
        this.templateEngine = buildTemplateEngine();
    }

    @Override
    public void sendReset(String toEmail, String nickname, UUID token) {
        String resetUrl = baseUrl + "/reset-senha?token=" + token;

        Context ctx = new Context(Locale.forLanguageTag("pt-BR"));
        ctx.setVariable("nickname", nickname == null || nickname.isBlank() ? "" : nickname);
        ctx.setVariable("resetUrl", resetUrl);

        String html = templateEngine.process(HTML_TEMPLATE, ctx);
        String plain = templateEngine.process(TEXT_TEMPLATE, ctx);

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(SUBJECT);
            helper.setText(plain, html);

            mailSender.send(mime);
            log.info("password reset email sent to={}", toEmail);
        } catch (MessagingException e) {
            // Sem logar token nem corpo — só o destinatário e a causa.
            log.error("failed to send password reset email to={} cause={}",
                    toEmail, e.getClass().getSimpleName());
            throw new IllegalStateException("Falha ao enviar e-mail de redefinição de senha", e);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private static TemplateEngine buildTemplateEngine() {
        TemplateEngine engine = new TemplateEngine();

        // Resolver de texto-plano (.txt) — captura nomes terminados em
        // "-plain" (ex.: email/password-reset-plain). Definido com ordem menor
        // para ser tentado primeiro.
        ClassLoaderTemplateResolver textResolver = new ClassLoaderTemplateResolver();
        textResolver.setPrefix("templates/");
        textResolver.setSuffix(".txt");
        textResolver.setTemplateMode(TemplateMode.TEXT);
        textResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        textResolver.setResolvablePatterns(Set.of("email/*-plain"));
        textResolver.setCheckExistence(true);
        textResolver.setCacheable(false);
        textResolver.setOrder(1);
        engine.addTemplateResolver(textResolver);

        // Resolver HTML (.html) — captura demais templates de e-mail.
        ClassLoaderTemplateResolver htmlResolver = new ClassLoaderTemplateResolver();
        htmlResolver.setPrefix("templates/");
        htmlResolver.setSuffix(".html");
        htmlResolver.setTemplateMode(TemplateMode.HTML);
        htmlResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        htmlResolver.setResolvablePatterns(Set.of("email/*"));
        htmlResolver.setCheckExistence(true);
        htmlResolver.setCacheable(false);
        htmlResolver.setOrder(2);
        engine.addTemplateResolver(htmlResolver);

        return engine;
    }
}
