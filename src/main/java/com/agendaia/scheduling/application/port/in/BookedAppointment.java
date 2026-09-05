package com.agendaia.scheduling.application.port.in;

import java.time.LocalDateTime;
import java.util.UUID;

/** Resultado de {@link BookAppointmentUseCase} — o que a tela de sucesso (US-5) precisa mostrar. */
public record BookedAppointment(UUID id, String serviceName, LocalDateTime startsAt) {}
