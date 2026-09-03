package com.agendaia.organization.application.port.in;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Uma linha da lista de jornada — já com o nome do profissional resolvido
 * (não id cru), o template não precisa de lógica.
 */
public record WorkScheduleView(UUID id, String professionalName, DayOfWeek dayOfWeek, LocalTime startsAt, LocalTime endsAt) {}
