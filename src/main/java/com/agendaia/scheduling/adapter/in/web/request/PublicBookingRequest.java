package com.agendaia.scheduling.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * O que o formulário de confirmação envia.
 *
 * <p>{@code website} é o honeypot (BR-7) — campo invisível ao humano via
 * CSS, nunca {@code type="hidden"} (bots reconhecem e ignoram campos
 * ocultos com facilidade). Deliberadamente sem {@code @jakarta.validation}
 * nele: um valor preenchido não deve virar erro de campo visível, só
 * fazer o controller recusar em silêncio.
 */
public record PublicBookingRequest(
        @NotBlank(message = "Informe seu nome")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String name,
        @NotBlank(message = "Informe seu telefone")
        @Pattern(regexp = "^\\+?\\d{8,15}$", message = "Telefone fora do formato aceito")
        String phone,
        String website) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public PublicBookingRequest() {
        this("", "", "");
    }

    public boolean isHoneypotFilled() {
        return website != null && !website.isBlank();
    }
}
