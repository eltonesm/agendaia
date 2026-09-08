package com.agendaia.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring — SimpleMeterRegistry (Micrometer, em memória) basta para os três contadores. */
class SchedulingMetricsTest {

    private SimpleMeterRegistry registry;
    private SchedulingMetrics metrics;

    @BeforeEach
    void montar() {
        registry = new SimpleMeterRegistry();
        metrics = new SchedulingMetrics(registry);
    }

    private double valor(String nome) {
        var counter = registry.find(nome).counter();
        return counter == null ? 0.0 : counter.count();
    }

    @Test
    @DisplayName("appointmentCreated() incrementa so o contador de criado")
    void appointmentCreatedIncrementaSoCriado() {
        metrics.appointmentCreated();

        assertThat(valor("agendaia.appointments.booked")).isEqualTo(1.0);
        assertThat(valor("agendaia.appointments.cancelled")).isEqualTo(0.0);
        assertThat(valor("agendaia.appointments.slot_conflict")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("appointmentCancelled() incrementa so o contador de cancelado")
    void appointmentCancelledIncrementaSoCancelado() {
        metrics.appointmentCancelled();

        assertThat(valor("agendaia.appointments.cancelled")).isEqualTo(1.0);
        assertThat(valor("agendaia.appointments.booked")).isEqualTo(0.0);
        assertThat(valor("agendaia.appointments.slot_conflict")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("slotConflict() incrementa so o contador de falha por conflito")
    void slotConflictIncrementaSoFalhaPorConflito() {
        metrics.slotConflict();

        assertThat(valor("agendaia.appointments.slot_conflict")).isEqualTo(1.0);
        assertThat(valor("agendaia.appointments.booked")).isEqualTo(0.0);
        assertThat(valor("agendaia.appointments.cancelled")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("contadores comecam em zero e sao cumulativos")
    void contadoresComecamEmZeroESaoCumulativos() {
        assertThat(valor("agendaia.appointments.booked")).isEqualTo(0.0);

        metrics.appointmentCreated();
        metrics.appointmentCreated();

        assertThat(valor("agendaia.appointments.booked")).isEqualTo(2.0);
    }
}
