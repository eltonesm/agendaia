package com.agendaia.organization.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Dados do cadastro de uma faixa de jornada.
 *
 * <p>Nenhum campo de tenant — quem determina o estabelecimento é a sessão, no
 * {@code TenantContext}, nunca o que chega no formulário (DD-1).
 */
public record RegisterWorkScheduleCommand(UUID professionalId, DayOfWeek dayOfWeek, LocalTime startsAt, LocalTime endsAt) {}
