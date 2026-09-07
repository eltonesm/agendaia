package com.agendaia.scheduling.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * O que o formulário de reagendamento do painel envia (US-4, agenda-
 * profissional, TODO-008). {@code serviceOfferingId} pode ser uma oferta de
 * outro profissional — reagendar também troca de profissional (decisão do
 * dono do produto).
 */
public record RescheduleRequest(
        @NotNull(message = "Escolha uma oferta") UUID serviceOfferingId,
        @NotNull(message = "Escolha a data") LocalDate date,
        @NotNull(message = "Escolha o horário") LocalTime time) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public RescheduleRequest() {
        this(null, null, null);
    }
}
