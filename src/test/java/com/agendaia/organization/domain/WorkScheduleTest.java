package com.agendaia.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring e sem banco, mesmo sendo entidade JPA. */
class WorkScheduleTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());
    private static final UUID PROFESSIONAL_ID = UuidV7.generate();

    @Test
    @DisplayName("nasce ativa, com id UUIDv7 e os dados informados")
    void nasceAtiva() {
        var faixa = WorkSchedule.register(
                TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

        assertThat(faixa.id()).isNotNull();
        assertThat(faixa.id().version()).isEqualTo(7);
        assertThat(faixa.isActive()).isTrue();
        assertThat(faixa.tenantId()).isEqualTo(TENANT);
        assertThat(faixa.professionalId()).isEqualTo(PROFESSIONAL_ID);
        assertThat(faixa.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(faixa.startsAt()).isEqualTo(LocalTime.of(8, 0));
        assertThat(faixa.endsAt()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("recusa nascer sem estabelecimento")
    void recusaSemTenant() {
        assertThatThrownBy(() -> WorkSchedule.register(
                        null, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    @DisplayName("recusa nascer sem profissional")
    void recusaSemProfissional() {
        assertThatThrownBy(() -> WorkSchedule.register(
                        TENANT, null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("profissional");
    }

    @Test
    @DisplayName("recusa fim antes ou igual ao início")
    void recusaFimAntesOuIgualAoInicio() {
        assertThatThrownBy(() -> WorkSchedule.register(
                        TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(8, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("depois do início");

        assertThatThrownBy(() -> WorkSchedule.register(
                        TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(8, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("depois do início");
    }

    @Test
    @DisplayName("duas faixas que se sobrepõem: overlaps() detecta")
    void detectaSobreposicao() {
        var manha = WorkSchedule.register(
                TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        var sobreposta = WorkSchedule.register(
                TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(14, 0));

        assertThat(manha.overlaps(sobreposta)).isTrue();
        assertThat(sobreposta.overlaps(manha)).isTrue();
    }

    @Test
    @DisplayName("faixas encostadas (fim de uma = início da outra) não se sobrepõem — mecanismo do almoço")
    void faixasEncostadasNaoSeSobrepoem() {
        var manha = WorkSchedule.register(
                TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        var tarde = WorkSchedule.register(
                TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(18, 0));

        assertThat(manha.overlaps(tarde)).isFalse();
        assertThat(tarde.overlaps(manha)).isFalse();
    }

    @Test
    @DisplayName("desativar não apaga — o histórico continua íntegro (ADR 0011)")
    void desativarNaoApaga() {
        var faixa = WorkSchedule.register(
                TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

        faixa.deactivate();

        assertThat(faixa.isActive()).isFalse();
        assertThat(faixa.id()).isNotNull();
    }

    @Test
    @DisplayName("igualdade por identidade")
    void igualdadePorId() {
        var faixa = WorkSchedule.register(
                TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

        assertThat(faixa).isEqualTo(faixa);
        assertThat(faixa)
                .isNotEqualTo(WorkSchedule.register(
                        TENANT, PROFESSIONAL_ID, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }
}
