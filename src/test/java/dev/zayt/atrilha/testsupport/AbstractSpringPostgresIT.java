package dev.zayt.atrilha.testsupport;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base para {@code @SpringBootTest} de integração sobre o Postgres singleton
 * ({@link AbstractPostgresIT}) que precisam de isolamento de <b>dados</b> entre
 * métodos.
 *
 * <p><b>Por que isto substitui {@code @DirtiesContext}?</b> Antes, as classes de
 * IT usavam {@code @DirtiesContext} (várias com {@code AFTER_EACH_TEST_METHOD})
 * para garantir que o estado do banco não vazasse entre testes. Mas
 * {@code @DirtiesContext} destrói o contexto inteiro do Spring — reconstruí-lo a
 * cada método/classe é o que dominava o tempo da suíte.</p>
 *
 * <p>A limpeza aqui é cirúrgica: um {@code TRUNCATE} de todas as tabelas do schema
 * {@code public} (exceto {@code flyway_schema_history}) antes de cada teste. Como
 * é um {@code @BeforeEach} da superclasse, roda <b>antes</b> de qualquer
 * {@code @BeforeEach} da subclasse. O contexto do Spring é preservado e reaproveitado
 * pelo cache entre classes de mesma configuração.</p>
 *
 * <p>As contas-semente de login são <b>em memória</b> (ver
 * {@code InMemoryLoginAccountQuery}), então o {@code TRUNCATE} não as remove.</p>
 *
 * <p>Classes com {@code @Transactional} continuam funcionando: o {@code TRUNCATE}
 * participa da transação do teste (e some no rollback), mas cada teste ainda começa
 * com o banco visivelmente limpo.</p>
 */
public abstract class AbstractSpringPostgresIT extends AbstractPostgresIT {

    @Autowired(required = false)
    private DataSource dataSource;

    @BeforeEach
    protected void cleanDatabaseBeforeEach() {
        if (dataSource == null) {
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' "
                        + "AND tablename <> 'flyway_schema_history'",
                String.class);
        if (tables.isEmpty()) {
            return;
        }
        String joined = tables.stream()
                .map(t -> "\"" + t + "\"")
                .collect(Collectors.joining(", "));
        jdbc.execute("TRUNCATE TABLE " + joined + " RESTART IDENTITY CASCADE");
    }
}
