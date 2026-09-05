package com.agendaia.scheduling.adapter.out.persistence;

import com.agendaia.scheduling.domain.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data cru — nunca exposto fora deste pacote. {@code scheduling.application} só conhece {@code AppointmentRepository}. */
interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, UUID> {

    /** BR-9: teto de agendamentos futuros ativos por telefone (via customerId). */
    long countByTenantIdAndCustomerIdAndStatusAndStartsAtAfter(
            UUID tenantId, UUID customerId, AppointmentStatus status, Instant agora);
}
