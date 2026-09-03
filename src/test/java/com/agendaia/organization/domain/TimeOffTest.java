package com.agendaia.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring e sem banco, mesmo sendo entidade JPA. */
class TimeOffTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());
    private static final UUID PROFESSIONAL_ID = UuidV7.generate();
    private static final Instant INICIO = Instant.now();
    private static final Instant FIM = INICIO.plus(1, ChronoUnit.DAYS);

    @Test
    @DisplayName("nasce ativo, com id UUIDv7 e os dados informados, de um profissional específico")
    void nasceAtivoDeUmProfissional() {
        var bloqueio = TimeOff.register(TENANT, PROFESSIONAL_ID, INICIO, FIM, "Consulta médica");

        assertThat(bloqueio.id()).isNotNull();
        assertThat(bloqueio.id().version()).isEqualTo(7);
        assertThat(bloqueio.isActive()).isTrue();
        assertThat(bloqueio.tenantId()).isEqualTo(TENANT);
        assertThat(bloqueio.professionalId()).isEqualTo(PROFESSIONAL_ID);
        assertThat(bloqueio.startsAt()).isEqualTo(INICIO);
        assertThat(bloqueio.endsAt()).isEqualTo(FIM);
        assertThat(bloqueio.reason()).isEqualTo("Consulta médica");
    }

    @Test
    @DisplayName("professionalId nulo é aceito — vale para o estabelecimento inteiro (DD-3)")
    void professionalIdNuloValeParaOEstabelecimentoInteiro() {
        var feriado = TimeOff.register(TENANT, null, INICIO, FIM, "Natal");

        assertThat(feriado.professionalId()).isNull();
    }

    @Test
    @DisplayName("motivo é opcional")
    void motivoEOpcional() {
        var bloqueio = TimeOff.register(TENANT, PROFESSIONAL_ID, INICIO, FIM, null);

        assertThat(bloqueio.reason()).isNull();
    }

    @Test
    @DisplayName("motivo em branco vira nulo, não string vazia")
    void motivoEmBrancoViraNulo() {
        var bloqueio = TimeOff.register(TENANT, PROFESSIONAL_ID, INICIO, FIM, "   ");

        assertThat(bloqueio.reason()).isNull();
    }

    @Test
    @DisplayName("recusa nascer sem estabelecimento")
    void recusaSemTenant() {
        assertThatThrownBy(() -> TimeOff.register(null, PROFESSIONAL_ID, INICIO, FIM, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    @DisplayName("recusa fim antes ou igual ao início")
    void recusaFimAntesOuIgualAoInicio() {
        assertThatThrownBy(() -> TimeOff.register(TENANT, PROFESSIONAL_ID, FIM, INICIO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("depois do início");

        assertThatThrownBy(() -> TimeOff.register(TENANT, PROFESSIONAL_ID, INICIO, INICIO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("depois do início");
    }

    @Test
    @DisplayName("desativar não apaga — o histórico continua íntegro (ADR 0011)")
    void desativarNaoApaga() {
        var bloqueio = TimeOff.register(TENANT, PROFESSIONAL_ID, INICIO, FIM, null);

        bloqueio.deactivate();

        assertThat(bloqueio.isActive()).isFalse();
        assertThat(bloqueio.id()).isNotNull();
    }

    @Test
    @DisplayName("igualdade por identidade")
    void igualdadePorId() {
        var bloqueio = TimeOff.register(TENANT, PROFESSIONAL_ID, INICIO, FIM, null);

        assertThat(bloqueio).isEqualTo(bloqueio);
        assertThat(bloqueio).isNotEqualTo(TimeOff.register(TENANT, PROFESSIONAL_ID, INICIO, FIM, null));
    }
}
