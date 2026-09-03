package com.agendaia.organization.adapter.in.web.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** O que o formulário de cadastro de horário de funcionamento envia. */
public record RegisterBusinessOperatingHoursRequest(
        @NotNull(message = "Selecione o dia da semana") DayOfWeek dayOfWeek,
        @NotNull(message = "Informe a abertura") LocalTime opensAt,
        @NotNull(message = "Informe o fechamento") LocalTime closesAt) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public RegisterBusinessOperatingHoursRequest() {
        this(null, null, null);
    }

    /**
     * Cross-field: Bean Validation não tem anotação pronta para "um campo
     * depois do outro". {@code isXxx()} vira a propriedade {@code xxx} para
     * o Jakarta Validation.
     */
    @AssertTrue(message = "Fechamento deve ser depois da abertura")
    public boolean isValidRange() {
        return opensAt == null || closesAt == null || closesAt.isAfter(opensAt);
    }
}
