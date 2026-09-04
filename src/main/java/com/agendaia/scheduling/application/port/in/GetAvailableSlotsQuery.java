package com.agendaia.scheduling.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code professionalId} não é campo aqui de propósito (DD-6): é derivado da
 * {@code ServiceOfferingRef} resolvida a partir de {@code serviceOfferingId}
 * — evita a possibilidade de os dois não baterem.
 */
public record GetAvailableSlotsQuery(UUID serviceOfferingId, LocalDate date) {}
