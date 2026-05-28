package dev.zayt.atrilha.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de testes de integração que compartilham UM único Postgres por JVM.
 *
 * <p><b>Por que singleton?</b> Antes, cada classe {@code *IT} declarava o seu
 * próprio {@code @Container static PostgreSQLContainer} via {@code @Testcontainers}.
 * Isso subia e derrubava ~23 containers Postgres distintos por execução, e — pior —
 * cada container ganhava uma URL JDBC aleatória, fragmentando o cache de contexto
 * do Spring (cada classe virava um contexto novo). O resultado eram os ~3 minutos
 * de suíte.</p>
 *
 * <p>Aqui o container é iniciado <b>uma vez</b> num bloco {@code static} (padrão
 * "singleton container" recomendado pela documentação do Testcontainers) e nunca
 * é fechado manualmente — o Ryuk do Testcontainers o remove ao fim da JVM. Como a
 * URL passa a ser estável e idêntica para todas as classes, o {@code @DynamicPropertySource}
 * herdado produz sempre as mesmas propriedades e o cache de contexto do Spring volta
 * a funcionar.</p>
 *
 * <p><b>Sobre {@code reuse} do Testcontainers:</b> não ativamos
 * {@code testcontainers.reuse.enable=true}. O {@code mvn verify} roda em duas JVMs
 * separadas (surefire para {@code *Test}, failsafe para {@code *IT}); um container
 * "reusable" não sobrevive de forma confiável a essa troca de JVM e leva a
 * {@code Connection refused} na fase failsafe. O ganho real vem do singleton
 * <b>por JVM</b> acima, que já é estável.</p>
 *
 * <p>Testes que tocam o esquema/dados via Spring devem estender
 * {@link AbstractSpringPostgresIT}, que adiciona limpeza de tabelas entre métodos.
 * Esta classe sozinha serve para quem só precisa da URL do container.</p>
 */
public abstract class AbstractPostgresIT {

    protected static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
                .withDatabaseName("atrilha")
                .withUsername("atrilha")
                .withPassword("atrilha");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
