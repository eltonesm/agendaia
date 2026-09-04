package com.agendaia.scheduling.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um início possível, já validado contra jornada, bloqueios e agendamentos
 * (glossário). Value object — <strong>calculado, nunca persistido</strong>
 * (BR-1).
 *
 * <p>{@code endsAt} é {@code startsAt} mais a duração da oferta, sem o
 * {@code bufferMinutes} — o intervalo é reservado depois do atendimento, não
 * faz parte do horário mostrado ao cliente.
 */
public record AvailableSlot(UUID professionalId, UUID serviceOfferingId, LocalDateTime startsAt, LocalDateTime endsAt) {

    public AvailableSlot {
        if (professionalId == null || serviceOfferingId == null) {
            throw new IllegalArgumentException("horário disponível precisa de profissional e oferta");
        }
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("horário disponível precisa de início e fim válidos");
        }
    }
}
