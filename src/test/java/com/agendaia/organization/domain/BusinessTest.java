package com.agendaia.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Sem Spring e sem banco, mesmo sendo entidade JPA. */
class BusinessTest {

    private static final Instant AGORA = Instant.parse("2026-08-30T12:00:00Z");

    @Test
    @DisplayName("nasce ativo, com id gerado e fuso padrão")
    void nasceAtivo() {
        var business = Business.register("Barbearia do João", "barbearia-do-joao");

        assertThat(business.id()).isNotNull();
        assertThat(business.isActive()).isTrue();
        assertThat(business.timezone()).isEqualTo(Business.FUSO_PADRAO);
        assertThat(business.name()).isEqualTo("Barbearia do João");
    }

    @Test
    @DisplayName("o id é UUIDv7 — ordenado no tempo, não aleatório")
    void idEUuidV7() {
        var business = Business.register("Barbearia do João", "barbearia-do-joao");

        assertThat(business.id().version()).isEqualTo(7);
    }

    @Test
    @DisplayName("o id do estabelecimento é o tenant")
    void idEOTenant() {
        var business = Business.register("Barbearia do João", "barbearia-do-joao");

        assertThat(business.tenantId().value()).isEqualTo(business.id());
    }

    @Test
    @DisplayName("remove espaços das pontas do nome")
    void limpaONome() {
        var business = Business.register("  Barbearia do João  ", "barbearia-do-joao");

        assertThat(business.name()).isEqualTo("Barbearia do João");
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "  ", ""})
    @DisplayName("recusa nome curto demais")
    void recusaNomeCurto(String nome) {
        assertThatThrownBy(() -> Business.register(nome, "barbearia-do-joao"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 2 e 120");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "-invalido", "Maiuscula", "com espaco"})
    @DisplayName("recusa slug fora do formato, antes de o banco recusar")
    void recusaSlugInvalido(String slug) {
        assertThatThrownBy(() -> Business.register("Barbearia do João", slug))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formato");
    }

    @Test
    @DisplayName("recusa fuso inexistente na criação, não meses depois no cálculo")
    void recusaFusoInvalido() {
        assertThatThrownBy(() ->
                        Business.register("Barbearia", "barbearia", "America/Nao_Existe", AGORA))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("desativar não apaga — o histórico continua íntegro (ADR 0011)")
    void desativarNaoApaga() {
        var business = Business.register("Barbearia do João", "barbearia-do-joao");

        business.deactivate();

        assertThat(business.isActive()).isFalse();
        assertThat(business.id()).isNotNull();
        assertThat(business.slug()).isEqualTo("barbearia-do-joao");
    }

    @Test
    @DisplayName("toString não vaza dado além de id e slug — pode ir para log")
    void toStringEnxuto() {
        var business = Business.register("Barbearia do João", "barbearia-do-joao");

        assertThat(business.toString())
                .contains("barbearia-do-joao")
                .doesNotContain("America/Sao_Paulo");
    }
}
