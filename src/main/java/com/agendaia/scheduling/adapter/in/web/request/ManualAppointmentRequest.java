package com.agendaia.scheduling.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * O que o formulário de criação manual do painel envia (US-2, agenda-
 * profissional, TODO-008). Sem honeypot nem rate limit — quem preenche já
 * está autenticado como dono (DD-1).
 */
public record ManualAppointmentRequest(
        @NotNull(message = "Escolha uma oferta") UUID serviceOfferingId,
        @NotNull(message = "Escolha a data") LocalDate date,
        @NotNull(message = "Escolha o horário") LocalTime time,
        @NotBlank(message = "Informe o nome do cliente")
                @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
                String customerName,
        @NotBlank(message = "Informe o telefone do cliente")
                @Pattern(regexp = "^\\+?\\d{8,15}$", message = "Telefone fora do formato aceito")
                String customerPhone) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public ManualAppointmentRequest() {
        this(null, null, null, "", "");
    }
}
