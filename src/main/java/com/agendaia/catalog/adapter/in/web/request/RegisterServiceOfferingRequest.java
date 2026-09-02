package com.agendaia.catalog.adapter.in.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/** O que o formulário de cadastro de oferta envia. */
public record RegisterServiceOfferingRequest(
        @NotNull(message = "Selecione o serviço") UUID serviceId,
        @NotNull(message = "Selecione o profissional") UUID professionalId,
        @Positive(message = "A duração deve ser maior que zero") int durationMinutes,
        @NotNull(message = "Informe o preço")
        @DecimalMin(value = "0.0", message = "O preço não pode ser negativo")
        BigDecimal price,
        @PositiveOrZero(message = "O intervalo não pode ser negativo") int bufferMinutes) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public RegisterServiceOfferingRequest() {
        this(null, null, 0, null, 0);
    }
}
