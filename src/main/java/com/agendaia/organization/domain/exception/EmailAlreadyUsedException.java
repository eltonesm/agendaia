package com.agendaia.organization.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * O e-mail já pertence a uma conta.
 *
 * <p>Diferente do login, aqui revelar que o e-mail existe é necessário: sem
 * isso o dono não entenderia por que o cadastro falhou, e tentaria de novo com
 * o mesmo e-mail indefinidamente.
 *
 * <p>A mensagem não confirma nem nega que a conta seja dele — apenas que aquele
 * e-mail não está disponível, e sugere entrar.
 */
public class EmailAlreadyUsedException extends DomainException {

    public EmailAlreadyUsedException() {
        super("Este e-mail já tem uma conta. Entre em vez de cadastrar.", "email");
    }
}
