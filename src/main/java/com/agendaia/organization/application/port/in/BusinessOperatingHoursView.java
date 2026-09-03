package com.agendaia.organization.application.port.in;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/** Uma linha da lista de horário de funcionamento. */
public record BusinessOperatingHoursView(UUID id, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {}
