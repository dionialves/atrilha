package dev.zayt.atrilha.accounts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expõe {@code /media/**} servindo o diretório
 * {@code ${app.media.upload-dir}} como estático.
 *
 * <p>Tornado público sem autenticação (avatar não é sensível); o nome do
 * arquivo é o {@code accountId} (UUID v4 — não enumerável). Quando a
 * estratégia mudar (S3 / CDN), basta substituir esta configuração.</p>
 */
@Configuration
class MediaResourceConfig implements WebMvcConfigurer {

    private final String uploadDir;

    MediaResourceConfig(@Value("${app.media.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
