package com.agendaia.organization.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Dados do cadastro de uma faixa de horário de funcionamento.
 *
 * <p>Nenhum campo de tenant — quem determina o estabelecimento é a sessão, no
 * {@code TenantContext}, nunca o que chega no formulário (DD-1 da TODO-002,
 * estendido aqui).
 */
public record RegisterBusinessOperatingHoursCommand(DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {}
