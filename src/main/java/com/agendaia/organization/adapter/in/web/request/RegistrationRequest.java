package com.agendaia.organization.adapter.in.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * O que o formulário de cadastro envia.
 *
 * <p>Três campos mais o link, que chega preenchido pelo navegador e é editável.
 * Cada campo a mais é abandono — a spec funcional pediu o mínimo.
 *
 * <p>As restrições aqui são de <strong>formato</strong>. Disponibilidade do link
 * e do e-mail depende do banco e é decidida no caso de uso.
 */
public record RegistrationRequest(
        @NotBlank(message = "Informe o nome do estabelecimento")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String businessName,

        @NotBlank(message = "Informe o endereço do seu link")
        @Size(min = 3, max = 60, message = "O link deve ter entre 3 e 60 caracteres")
        @Pattern(
                regexp = "^[a-z0-9]([a-z0-9-]*[a-z0-9])?$",
                message = "Use apenas letras minúsculas, números e hífen")
        String slug,

        @NotBlank(message = "Informe seu e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 254, message = "E-mail longo demais")
        String email,

        @NotBlank(message = "Informe uma senha")
        @Size(min = 8, message = "A senha precisa de pelo menos 8 caracteres")
        String password) {

    /** Construtor vazio para o Thymeleaf renderizar o formulário na primeira visita. */
    public RegistrationRequest() {
        this("", "", "", "");
    }

    /**
     * Nunca imprime a senha: o record padrão imprimiria todos os campos, e este
     * objeto vai para o model do Thymeleaf.
     */
    @Override
    public String toString() {
        return "RegistrationRequest[businessName=%s, slug=%s]".formatted(businessName, slug);
    }
}
