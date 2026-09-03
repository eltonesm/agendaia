package com.agendaia.organization.adapter.in.web.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O que o formulário de cadastro de bloqueio envia.
 *
 * <p>{@code professionalId} é o único campo sem {@code @NotNull} de
 * propósito: vazio significa "estabelecimento inteiro" (DD-3), não erro de
 * formulário. Datas chegam como {@link LocalDateTime} (hora local, sem fuso
 * — {@code <input type="datetime-local">}); o controller converte para
 * {@link java.time.Instant} usando o fuso do servidor antes de montar o
 * comando.
 */
public record RegisterTimeOffRequest(
        UUID professionalId,
        @NotNull(message = "Informe o início") LocalDateTime startsAt,
        @NotNull(message = "Informe o fim") LocalDateTime endsAt,
        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres") String reason) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public RegisterTimeOffRequest() {
        this(null, null, null, "");
    }

    @AssertTrue(message = "Fim deve ser depois do início")
    public boolean isValidRange() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
