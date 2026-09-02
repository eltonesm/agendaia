package com.agendaia.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Sem Spring e sem banco, mesmo sendo entidade JPA. */
class ServiceTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());

    @Test
    @DisplayName("nasce ativo, com id UUIDv7, nome e descrição informados")
    void nasceAtivo() {
        var servico = Service.register(TENANT, "Corte de Cabelo", "Corte na tesoura ou máquina");

        assertThat(servico.id()).isNotNull();
        assertThat(servico.id().version()).isEqualTo(7);
        assertThat(servico.isActive()).isTrue();
        assertThat(servico.name()).isEqualTo("Corte de Cabelo");
        assertThat(servico.description()).isEqualTo("Corte na tesoura ou máquina");
        assertThat(servico.tenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("descrição é opcional")
    void descricaoEOpcional() {
        var servico = Service.register(TENANT, "Corte de Cabelo", null);

        assertThat(servico.description()).isNull();
    }

    @Test
    @DisplayName("descrição em branco vira nulo, não string vazia")
    void descricaoEmBrancoViraNulo() {
        var servico = Service.register(TENANT, "Corte de Cabelo", "   ");

        assertThat(servico.description()).isNull();
    }

    @Test
    @DisplayName("recusa nascer sem estabelecimento")
    void recusaSemTenant() {
        assertThatThrownBy(() -> Service.register(null, "Corte de Cabelo", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    @DisplayName("remove espaços das pontas do nome")
    void limpaONome() {
        var servico = Service.register(TENANT, "  Corte de Cabelo  ", null);

        assertThat(servico.name()).isEqualTo("Corte de Cabelo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "  ", ""})
    @DisplayName("recusa nome curto demais")
    void recusaNomeCurto(String nome) {
        assertThatThrownBy(() -> Service.register(TENANT, nome, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 2 e 120");
    }

    @Test
    @DisplayName("recusa descrição longa demais")
    void recusaDescricaoLonga() {
        var descricaoLonga = "x".repeat(501);

        assertThatThrownBy(() -> Service.register(TENANT, "Corte de Cabelo", descricaoLonga))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("desativar não apaga — o histórico continua íntegro (ADR 0011)")
    void desativarNaoApaga() {
        var servico = Service.register(TENANT, "Corte de Cabelo", null);

        servico.deactivate();

        assertThat(servico.isActive()).isFalse();
        assertThat(servico.id()).isNotNull();
        assertThat(servico.name()).isEqualTo("Corte de Cabelo");
    }

    @Test
    @DisplayName("igualdade por identidade, não por nome")
    void igualdadePorId() {
        var servico = Service.register(TENANT, "Corte de Cabelo", null);

        assertThat(servico).isEqualTo(servico);
        assertThat(servico).isNotEqualTo(Service.register(TENANT, "Corte de Cabelo", null));
    }
}
