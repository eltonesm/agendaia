package com.simboraagendar.scheduling.adapter.out.persistence;

import com.simboraagendar.scheduling.domain.AppointmentStatus;
import com.simboraagendar.scheduling.domain.PaymentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data cru — nunca exposto fora deste pacote. {@code scheduling.application} só conhece {@code AppointmentRepository}. */
interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, UUID> {

    /** Resolução por id, sempre revalidada por tenant (confirmacao-e-cancelamento, DD-1). */
    Optional<AppointmentJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * Grava só status e updatedAt — nunca via save()/merge, que
     * sobrescreveria createdAt (confirmacao-e-cancelamento, DD-4).
     */
    @Modifying
    @Query("""
            update AppointmentJpaEntity a
               set a.status = :status, a.updatedAt = :agora
             where a.tenantId = :tenantId and a.id = :id
            """)
    int updateStatus(
            @Param("tenantId") UUID tenantId,
            @Param("id") UUID id,
            @Param("status") AppointmentStatus status,
            @Param("agora") Instant agora);

    /**
     * BR-9: teto de agendamentos futuros ativos por telefone (via
     * customerId) — considera SCHEDULED e CONFIRMED (BR-6 de
     * confirmacao-e-cancelamento, DD-6).
     */
    long countByTenantIdAndCustomerIdAndStatusInAndStartsAtAfter(
            UUID tenantId, UUID customerId, Collection<AppointmentStatus> statuses, Instant agora);

    /**
     * Agendamentos ativos do profissional que se sobrepõem ao dia — usado
     * para excluir da disponibilidade calculada (achado durante o TASK-006
     * de pagina-publica-agendamento, mesma técnica de {@code
     * TimeOffRepository.findOverlapping}, organization).
     */
    @Query("""
            select a from AppointmentJpaEntity a
            where a.tenantId = :tenantId and a.professionalId = :professionalId
              and a.status in ('SCHEDULED', 'CONFIRMED')
              and a.startsAt < :dayEnd and a.endsAt > :dayStart
            """)
    List<AppointmentJpaEntity> findOverlapping(
            @Param("tenantId") UUID tenantId,
            @Param("professionalId") UUID professionalId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd);

    /**
     * Todos os agendamentos do profissional no dia, de qualquer status —
     * diferente de {@link #findOverlapping}, que só traz os ativos (agenda-
     * profissional, TODO-008, DD-3).
     */
    @Query("""
            select a from AppointmentJpaEntity a
            where a.tenantId = :tenantId and a.professionalId = :professionalId
              and a.startsAt < :dayEnd and a.endsAt > :dayStart
            order by a.startsAt asc
            """)
    List<AppointmentJpaEntity> findByProfessionalAndDay(
            @Param("tenantId") UUID tenantId,
            @Param("professionalId") UUID professionalId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd);

    /**
     * Todos os agendamentos do tenant no dia, de qualquer profissional e
     * qualquer status — para os KPIs do painel (sistema-de-design-admin,
     * TODO-110, DD-6/DD-7). Mesma janela de {@link #findByProfessionalAndDay},
     * sem o filtro de profissional.
     */
    @Query("""
            select a from AppointmentJpaEntity a
            where a.tenantId = :tenantId
              and a.startsAt < :dayEnd and a.endsAt > :dayStart
            order by a.startsAt asc
            """)
    List<AppointmentJpaEntity> findByTenantAndDay(
            @Param("tenantId") UUID tenantId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd);

    /**
     * Grava só o status de pagamento e updatedAt — nunca via save()/merge,
     * que sobrescreveria createdAt (mesma disciplina de {@link #updateStatus},
     * gestao-de-clientes).
     */
    @Modifying
    @Query("""
            update AppointmentJpaEntity a
               set a.paymentStatus = :paymentStatus, a.updatedAt = :agora
             where a.tenantId = :tenantId and a.id = :id
            """)
    int updatePaymentStatus(
            @Param("tenantId") UUID tenantId,
            @Param("id") UUID id,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("agora") Instant agora);

    /**
     * Histórico completo do cliente — só COMPLETED (gestao-de-clientes,
     * US-2, BR-1/BR-5), mais recente primeiro.
     */
    List<AppointmentJpaEntity> findByTenantIdAndCustomerIdAndStatusOrderByStartsAtDesc(
            UUID tenantId, UUID customerId, AppointmentStatus status);

    /**
     * Atividade agregada por cliente, em lote — uma única consulta GROUP BY
     * (gestao-de-clientes, DD-2). appointment_customer_idx (tenant_id,
     * customer_id, status), da V8, cobre bem o filtro por lote de
     * customer_id + status.
     */
    @Query("""
            select a.customerId as customerId,
                   count(a) as visitCount,
                   sum(a.priceCents) as totalCents,
                   sum(case when a.paymentStatus = com.simboraagendar.scheduling.domain.PaymentStatus.ON_CREDIT
                            then a.priceCents else 0 end) as owedCents
              from AppointmentJpaEntity a
             where a.tenantId = :tenantId
               and a.customerId in :customerIds
               and a.status = com.simboraagendar.scheduling.domain.AppointmentStatus.COMPLETED
             group by a.customerId
            """)
    List<CustomerActivityProjection> findActivityByCustomerIds(
            @Param("tenantId") UUID tenantId, @Param("customerIds") Collection<UUID> customerIds);
}
