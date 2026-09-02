package com.agendaia.catalog.adapter.in.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * O que o formulário de cadastro de oferta envia.
 *
 * <p>{@code durationMinutes} é {@link Integer}, não {@code int}: zero não é
 * duração válida nenhuma, então o construtor vazio precisa deixar o campo
 * nulo — com primitivo, a primeira visita renderizaria "0" pré-preenchido no
 * lugar do placeholder, escondendo o exemplo. {@code bufferMinutes} não tem
 * esse problema — zero é o valor mais comum, então primitivo com padrão 0 é
 * a UX certa ali.
 */
public record RegisterServiceOfferingRequest(
        @NotNull(message = "Selecione o serviço") UUID serviceId,
        @NotNull(message = "Selecione o profissional") UUID professionalId,
        @NotNull(message = "Informe a duração")
        @Positive(message = "A duração deve ser maior que zero")
        Integer durationMinutes,
        @NotNull(message = "Informe o preço")
        @DecimalMin(value = "0.0", message = "O preço não pode ser negativo")
        BigDecimal price,
        @PositiveOrZero(message = "O intervalo não pode ser negativo") int bufferMinutes) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public RegisterServiceOfferingRequest() {
        this(null, null, null, null, 0);
    }
}
