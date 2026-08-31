package com.agendaia.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Sem Spring e sem banco, mesmo sendo entidade JPA. */
class ProfessionalTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());

    @Test
    @DisplayName("nasce ativo, com id UUIDv7 e o nome informado")
    void nasceAtivo() {
        var profissional = Professional.register(TENANT, "João da Silva");

        assertThat(profissional.id()).isNotNull();
        assertThat(profissional.id().version()).isEqualTo(7);
        assertThat(profissional.isActive()).isTrue();
        assertThat(profissional.name()).isEqualTo("João da Silva");
        assertThat(profissional.tenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("recusa nascer sem estabelecimento")
    void recusaSemTenant() {
        assertThatThrownBy(() -> Professional.register(null, "João da Silva"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    @DisplayName("remove espaços das pontas do nome")
    void limpaONome() {
        var profissional = Professional.register(TENANT, "  João da Silva  ");

        assertThat(profissional.name()).isEqualTo("João da Silva");
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "  ", ""})
    @DisplayName("recusa nome curto demais")
    void recusaNomeCurto(String nome) {
        assertThatThrownBy(() -> Professional.register(TENANT, nome))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 2 e 120");
    }

    @Test
    @DisplayName("dois profissionais do mesmo tenant podem ter o mesmo nome")
    void nomeDuplicadoEPermitido() {
        var primeiro = Professional.register(TENANT, "João da Silva");
        var segundo = Professional.register(TENANT, "João da Silva");

        assertThat(primeiro.id()).isNotEqualTo(segundo.id());
        assertThat(primeiro.name()).isEqualTo(segundo.name());
    }

    @Test
    @DisplayName("desativar não apaga — o histórico continua íntegro (ADR 0011)")
    void desativarNaoApaga() {
        var profissional = Professional.register(TENANT, "João da Silva");

        profissional.deactivate();

        assertThat(profissional.isActive()).isFalse();
        assertThat(profissional.id()).isNotNull();
        assertThat(profissional.name()).isEqualTo("João da Silva");
    }

    @Test
    @DisplayName("igualdade por identidade, não por nome")
    void igualdadePorId() {
        var profissional = Professional.register(TENANT, "João da Silva");

        assertThat(profissional).isEqualTo(profissional);
        assertThat(profissional).isNotEqualTo(Professional.register(TENANT, "João da Silva"));
    }
}
