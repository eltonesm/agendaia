package com.agendaia.scheduling.application.port.out;

import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.TimeRange;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    /**
     * Intervalos já ocupados por agendamento ativo ({@code SCHEDULED}/
     * {@code CONFIRMED}) do profissional na data, recortados às bordas do
     * dia — mesma técnica de {@code organization.api.AvailabilityDirectory
     * #blocksFor}. {@code GetAvailableSlotsHandler} os trata como mais um
     * bloqueio, junto com {@code TimeOff}: sem isso, a listagem de
     * horários livres nunca refletiria uma reserva já feita (achado durante
     * o TASK-006 de pagina-publica-agendamento).
     */
    List<TimeRange> findOccupiedRanges(TenantId tenantId, UUID professionalId, LocalDate date);
}
