package com.agendaia.organization.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * O link escolhido não pode ser usado.
 *
 * <p>Cobre três situações que o usuário não precisa distinguir: já está em uso
 * por outro estabelecimento, é palavra reservada, ou perdeu a corrida para um
 * cadastro simultâneo. A mensagem é a mesma nos três casos — saber qual é não
 * ajuda quem está preenchendo o formulário, e revelar que um slug específico
 * "existe" entrega informação sobre a base de clientes.
 */
public class SlugUnavailableException extends DomainException {

    private final String slug;

    public SlugUnavailableException(String slug) {
        super("Este link já está em uso. Escolha outro.", "slug");
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }
}
