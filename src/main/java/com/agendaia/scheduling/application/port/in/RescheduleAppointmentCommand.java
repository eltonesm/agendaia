package com.agendaia.scheduling.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code newServiceOfferingId} permite trocar de profissional, não só de
 * horário — cada oferta pertence a um único profissional, então escolher
 * outra oferta já é escolher outro profissional (US-4, agenda-profissional,
 * TODO-008; decisão explícita do dono do produto: reagendar não fica
 * restrito ao mesmo profissional).
 */
public record RescheduleAppointmentCommand(UUID appointmentId, UUID newServiceOfferingId, Instant newStartsAt) {}
