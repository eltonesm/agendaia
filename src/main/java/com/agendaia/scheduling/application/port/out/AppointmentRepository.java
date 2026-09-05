package com.agendaia.scheduling.application.port.out;

import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.shared.TenantId;
import java.time.Instant;
import java.util.UUID;

/**
 * Porta de saída de {@link Appointment} — regime completo (ADR 0002):
 * {@code scheduling.application} fala com esta interface, nunca com JPA
 * diretamente. Implementada por {@code adapter.out.persistence.AppointmentPersistenceAdapter}.
 */
public interface AppointmentRepository {

    /**
     * Grava o agendamento. A garantia contra overbooking (BR-4/ADR 0005) é
     * do banco: uma violação da exclusion constraint vira
     * {@code SlotUnavailableException}, nunca uma exceção de persistência
     * crua.
     */
    Appointment save(Appointment appointment);

    /** Quantos agendamentos futuros ainda ativos ({@code SCHEDULED}) o cliente tem no tenant (BR-9). */
    long countFutureActive(TenantId tenantId, UUID customerId, Instant agora);
}
