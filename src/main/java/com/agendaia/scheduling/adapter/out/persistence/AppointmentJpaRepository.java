package com.agendaia.scheduling.adapter.out.persistence;

import com.agendaia.scheduling.domain.AppointmentStatus;
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
}
