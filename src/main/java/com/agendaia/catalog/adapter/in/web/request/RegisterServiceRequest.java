package com.agendaia.catalog.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O que o formulário de cadastro de serviço envia. */
public record RegisterServiceRequest(
        @NotBlank(message = "Informe o nome do serviço")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String name,
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres") String description) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public RegisterServiceRequest() {
        this("", "");
    }
}
