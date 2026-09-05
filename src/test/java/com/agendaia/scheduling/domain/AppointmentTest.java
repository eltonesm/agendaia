package com.agendaia.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring e sem banco. */
class AppointmentTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());
    private static final Instant STARTS_AT = Instant.parse("2026-09-07T11:00:00Z");
    private static final Instant ENDS_AT = STARTS_AT.plus(30, ChronoUnit.MINUTES);

    private static Appointment agendamentoValido() {
        return Appointment.schedule(
                TENANT,
                UuidV7.generate(),
                UuidV7.generate(),
                UuidV7.generate(),
                "Corte de Cabelo",
                30,
                new Money(3000),
                STARTS_AT,
                ENDS_AT);
    }

    @Test
    @DisplayName("nasce sempre SCHEDULED, com id UUIDv7 (BR-1)")
    void nasceSempreScheduled() {
        var agendamento = agendamentoValido();

        assertThat(agendamento.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(agendamento.id()).isNotNull();
        assertThat(agendamento.id().version()).isEqualTo(7);
    }

    @Test
    @DisplayName("guarda o retrato de serviço/duração/preço informado, não uma referência viva (BR-2)")
    void guardaORetrato() {
        var agendamento = agendamentoValido();

        assertThat(agendamento.serviceName()).isEqualTo("Corte de Cabelo");
        assertThat(agendamento.durationMinutes()).isEqualTo(30);
        assertThat(agendamento.price()).isEqualTo(new Money(3000));
        assertThat(agendamento.startsAt()).isEqualTo(STARTS_AT);
        assertThat(agendamento.endsAt()).isEqualTo(ENDS_AT);
    }

    @Test
    @DisplayName("recusa nascer sem estabelecimento")
    void recusaSemTenant() {
        assertThatThrownBy(() -> Appointment.schedule(
                        null,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    @DisplayName("recusa nascer sem profissional, oferta ou cliente")
    void recusaSemProfissionalOfertaOuCliente() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        null,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        null,
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        null,
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("recusa nascer sem o nome do serviço")
    void recusaSemNomeDoServico() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "  ",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome do serviço");
    }

    @Test
    @DisplayName("recusa duração zero ou negativa")
    void recusaDuracaoInvalida() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        0,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duração");
    }

    @Test
    @DisplayName("recusa nascer sem preço")
    void recusaSemPreco() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        null,
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preço");
    }

    @Test
    @DisplayName("recusa fim antes ou igual ao início")
    void recusaFimAntesOuIgualAoInicio() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        STARTS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("início e fim");

        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        STARTS_AT.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("igualdade por identidade")
    void igualdadePorId() {
        var agendamento = agendamentoValido();

        assertThat(agendamento).isEqualTo(agendamento);
        assertThat(agendamento).isNotEqualTo(agendamentoValido());
    }

    @Test
    @DisplayName("reconstitute preserva o status persistido, não força SCHEDULED")
    void reconstitutePreservaOStatus() {
        var id = UuidV7.generate();
        var agendamento = Appointment.reconstitute(
                id,
                TENANT,
                UuidV7.generate(),
                UuidV7.generate(),
                UuidV7.generate(),
                AppointmentStatus.CANCELLED,
                STARTS_AT,
                ENDS_AT,
                "Corte de Cabelo",
                30,
                new Money(3000));

        assertThat(agendamento.id()).isEqualTo(id);
        assertThat(agendamento.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }
}
