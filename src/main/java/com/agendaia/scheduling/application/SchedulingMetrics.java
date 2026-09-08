package com.agendaia.scheduling.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Três contadores de negócio (TODO-108, observabilidade): agendamentos
 * criados, cancelados e tentativas que falharam por conflito de horário.
 *
 * <p>Contadores globais do sistema, sem tag de tenant (decisão explícita
 * da spec funcional, BR-3 — um piloto só hoje). Vivem em memória,
 * reiniciando a zero a cada subida da aplicação, mesmo comportamento de
 * qualquer {@link Counter} do Prometheus.
 *
 * <p>Pacote-privada de propósito: só {@link BookAppointmentHandler},
 * {@link ManageAppointmentHandler} e {@link ProfessionalAgendaHandler} —
 * todos no mesmo pacote — a usam. Não pode viver em
 * {@code scheduling.domain} (ADR 0002, domínio puro não conhece
 * Micrometer).
 *
 * <p>Reagendamento não incrementa {@code created} nem {@code cancelled}
 * — é "mover", não "criar mais um" nem "desistir de um" (DD-3 da spec
 * técnica). Só falha por conflito no novo horário conta.
 */
@Component
class SchedulingMetrics {

    private final Counter created;
    private final Counter cancelled;
    private final Counter slotConflict;

    SchedulingMetrics(MeterRegistry registry) {
        // "agendaia.appointments.created" foi tentado primeiro e descartado:
        // o Prometheus/Micrometer trata o sufixo ".created" como a convenção
        // reservada do OpenMetrics para timestamp de criação de contador, e
        // a métrica saía como "agendaia_appointments_total" — sem "created"
        // nenhum, indistinguível de qualquer outro contador. "booked" evita
        // a colisão e continua na linguagem do domínio (glossário: reservar).
        created = Counter.builder("agendaia.appointments.booked").register(registry);
        cancelled = Counter.builder("agendaia.appointments.cancelled").register(registry);
        slotConflict = Counter.builder("agendaia.appointments.slot_conflict").register(registry);
    }

    void appointmentCreated() {
        created.increment();
    }

    void appointmentCancelled() {
        cancelled.increment();
    }

    void slotConflict() {
        slotConflict.increment();
    }
}
