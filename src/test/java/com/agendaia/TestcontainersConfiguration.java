package com.agendaia;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres real para os testes de integração.
 *
 * <p>Usa a <strong>mesma imagem do {@code compose.yaml}</strong>, de propósito:
 * testar contra uma versão diferente da que roda em desenvolvimento e produção
 * transformaria o teste numa verificação de outro sistema.
 *
 * <p>H2 não serve aqui em nenhuma hipótese — não implementa
 * {@code EXCLUDE USING gist}, que é a barreira contra overbooking do ADR 0005.
 *
 * <p>{@code @ServiceConnection} injeta URL, usuário e senha do container no
 * datasource automaticamente; não há configuração duplicada a manter.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("postgres:18-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withReuse(true);
    }
}
