package com.agendaia.organization.adapter.in.web.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/** O que o formulário de cadastro de jornada envia. */
public record RegisterWorkScheduleRequest(
        @NotNull(message = "Selecione o profissional") UUID professionalId,
        @NotNull(message = "Selecione o dia da semana") DayOfWeek dayOfWeek,
        @NotNull(message = "Informe o início") LocalTime startsAt,
        @NotNull(message = "Informe o fim") LocalTime endsAt) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public RegisterWorkScheduleRequest() {
        this(null, null, null, null);
    }

    @AssertTrue(message = "Fim deve ser depois do início")
    public boolean isValidRange() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
