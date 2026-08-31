package com.agendaia.organization.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O que o formulário de cadastro de profissional envia.
 *
 * <p>Um campo só — a spec funcional pediu o mínimo (Out of Scope: foto,
 * especialidade, telefone).
 */
public record RegisterProfessionalRequest(
        @NotBlank(message = "Informe o nome do profissional")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String name) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public RegisterProfessionalRequest() {
        this("");
    }
}
