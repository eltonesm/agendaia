package com.agendaia.scheduling.adapter.out.persistence;

import com.agendaia.scheduling.domain.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data cru — nunca exposto fora deste pacote. {@code scheduling.application} só conhece {@code AppointmentRepository}. */
interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, UUID> {

    /** BR-9: teto de agendamentos futuros ativos por telefone (via customerId). */
    long countByTenantIdAndCustomerIdAndStatusAndStartsAtAfter(
            UUID tenantId, UUID customerId, AppointmentStatus status, Instant agora);

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
}
