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
 *
 * <p>Quando há uma variação livre, ela entra na mensagem. Não distingue os três
 * casos acima: o usuário já sabe que o link dele não serve, e a sugestão poupa
 * a ele inventar outro na hora — que é onde o cadastro costuma ser abandonado.
 */
public class SlugUnavailableException extends DomainException {

    private final String slug;
    private final String suggestion;

    public SlugUnavailableException(String slug) {
        this(slug, null);
    }

    public SlugUnavailableException(String slug, String suggestion) {
        super(mensagem(suggestion), "slug");
        this.slug = slug;
        this.suggestion = suggestion;
    }

    private static String mensagem(String suggestion) {
        return suggestion == null || suggestion.isBlank()
                ? "Este link já está em uso. Escolha outro."
                : "Este link já está em uso. Que tal %s?".formatted(suggestion);
    }

    public String slug() {
        return slug;
    }

    /** A variação livre oferecida, ou {@code null} quando não houve nenhuma. */
    public String suggestion() {
        return suggestion;
    }
}
