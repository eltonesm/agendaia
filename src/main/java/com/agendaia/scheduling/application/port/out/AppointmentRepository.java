package com.agendaia.scheduling.application.port.out;

import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.PaymentStatus;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.TimeRange;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Um agendamento por id, sempre revalidado por tenant (BR-1/BR-7 de
     * confirmacao-e-cancelamento, TODO-007, DD-1) — vazio se pertencer a
     * outro tenant ou não existir.
     */
    Optional<Appointment> findByTenantIdAndId(TenantId tenantId, UUID id);

    /**
     * Grava só a mudança de status (+ {@code updatedAt}) — nunca via
     * {@link #save}, que sobrescreveria {@code createdAt} (DD-4 de
     * confirmacao-e-cancelamento).
     */
    void updateStatus(TenantId tenantId, UUID id, AppointmentStatus status, Instant agora);

    /**
     * Quantos agendamentos futuros ainda ativos ({@code SCHEDULED} ou
     * {@code CONFIRMED}, DD-6 de confirmacao-e-cancelamento) o cliente tem
     * no tenant (BR-9/BR-6).
     */
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

    /**
     * Todos os agendamentos do profissional na data, de qualquer status
     * (inclusive {@code CANCELLED} — ADR 0011, nada é apagado), para a
     * agenda do dono (agenda-profissional, TODO-008, DD-3). Ordenados por
     * {@code startsAt}.
     */
    List<Appointment> findByTenantIdAndProfessionalIdAndDate(TenantId tenantId, UUID professionalId, LocalDate date);

    /**
     * Todos os agendamentos do tenant na data, de qualquer profissional e
     * qualquer status — para os KPIs do painel (sistema-de-design-admin,
     * TODO-110, DD-6/DD-7).
     */
    List<Appointment> findByTenantIdAndDate(TenantId tenantId, LocalDate date);

    /**
     * Grava só o status de pagamento (+ {@code updatedAt}) — nunca via
     * {@link #save} (gestao-de-clientes, IDEA-006). Mesma forma de
     * {@link #updateStatus}, conceito independente (BR-2 da spec funcional).
     */
    void updatePaymentStatus(TenantId tenantId, UUID id, PaymentStatus paymentStatus, Instant agora);

    /**
     * Histórico completo de atendimentos do cliente — só {@code COMPLETED}
     * (BR-1/BR-5), mais recente primeiro. Usado no detalhe do cliente
     * (gestao-de-clientes, US-2); os totais são somados em memória sobre
     * esta lista, mesmo padrão de {@code DailyScheduleSummaryHandler}.
     */
    List<Appointment> findCompletedByTenantIdAndCustomerId(TenantId tenantId, UUID customerId);

    /**
     * Atividade agregada (visitas, gasto, em aberto) por cliente, em lote —
     * uma única consulta agrupada (GROUP BY), nunca uma por cliente
     * (gestao-de-clientes, DD-2). Cliente sem nenhum agendamento
     * {@code COMPLETED} simplesmente não aparece no mapa — quem chama trata
     * a ausência como "zero atividade" (BR-1).
     */
    Map<UUID, CustomerActivity> findActivityByCustomerIds(TenantId tenantId, Collection<UUID> customerIds);
}
