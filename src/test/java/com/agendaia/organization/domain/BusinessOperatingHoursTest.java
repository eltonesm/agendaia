package com.agendaia.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring e sem banco, mesmo sendo entidade JPA. */
class BusinessOperatingHoursTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());

    @Test
    @DisplayName("nasce ativa, com id UUIDv7 e os dados informados")
    void nasceAtiva() {
        var faixa = BusinessOperatingHours.register(
                TENANT, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0));

        assertThat(faixa.id()).isNotNull();
        assertThat(faixa.id().version()).isEqualTo(7);
        assertThat(faixa.isActive()).isTrue();
        assertThat(faixa.tenantId()).isEqualTo(TENANT);
        assertThat(faixa.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(faixa.opensAt()).isEqualTo(LocalTime.of(8, 0));
        assertThat(faixa.closesAt()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    @DisplayName("recusa nascer sem estabelecimento")
    void recusaSemTenant() {
        assertThatThrownBy(() -> BusinessOperatingHours.register(
                        null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    @DisplayName("recusa fechamento antes ou igual à abertura")
    void recusaFechamentoAntesOuIgualAAbertura() {
        assertThatThrownBy(() -> BusinessOperatingHours.register(
                        TENANT, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(8, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("depois da abertura");

        assertThatThrownBy(() -> BusinessOperatingHours.register(
                        TENANT, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(8, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("depois da abertura");
    }

    @Test
    @DisplayName("aceita várias faixas no mesmo dia — cada chamada é uma faixa independente")
    void aceitaVariasFaixasNoMesmoDia() {
        var manha = BusinessOperatingHours.register(TENANT, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        var tarde = BusinessOperatingHours.register(TENANT, DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(18, 0));

        assertThat(manha.id()).isNotEqualTo(tarde.id());
        assertThat(manha.dayOfWeek()).isEqualTo(tarde.dayOfWeek());
    }

    @Test
    @DisplayName("desativar não apaga — o histórico continua íntegro (ADR 0011)")
    void desativarNaoApaga() {
        var faixa = BusinessOperatingHours.register(TENANT, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0));

        faixa.deactivate();

        assertThat(faixa.isActive()).isFalse();
        assertThat(faixa.id()).isNotNull();
    }

    @Test
    @DisplayName("igualdade por identidade")
    void igualdadePorId() {
        var faixa = BusinessOperatingHours.register(TENANT, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0));

        assertThat(faixa).isEqualTo(faixa);
        assertThat(faixa)
                .isNotEqualTo(BusinessOperatingHours.register(
                        TENANT, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)));
    }
}
