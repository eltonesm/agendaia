package com.agendaia;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Teste de fumaça: prova que a aplicação sobe contra um Postgres real e que o
 * Flyway aplicou as migrations.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AgendaIaApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("o contexto sobe contra um Postgres real")
    void contextLoads() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("o Flyway aplicou as migrations")
    void flywayApplied() {
        var jdbc = new JdbcTemplate(dataSource);

        var aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);

        assertThat(aplicadas).isPositive();
    }

    @Test
    @DisplayName("btree_gist está instalada — sem ela a barreira contra overbooking não existe")
    void btreeGistInstalada() {
        var jdbc = new JdbcTemplate(dataSource);

        var instalada = jdbc.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'btree_gist'", Integer.class);

        assertThat(instalada)
                .as("a migration V1 deveria ter criado a extensão btree_gist")
                .isEqualTo(1);
    }
}
