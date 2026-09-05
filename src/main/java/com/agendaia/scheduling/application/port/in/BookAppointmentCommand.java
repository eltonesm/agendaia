package com.agendaia.scheduling.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code professionalId} não é campo aqui, mesmo raciocínio de
 * {@code GetAvailableSlotsQuery} (DD-6 de consultar-horarios-disponiveis):
 * é derivado da {@code ServiceOfferingRef} resolvida a partir de
 * {@code serviceOfferingId}, nunca aceito em paralelo.
 */
public record BookAppointmentCommand(UUID serviceOfferingId, Instant startsAt, String customerName, String customerPhone) {}
