package com.agendaia.catalog.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * O profissional informado não está entre os profissionais ativos do
 * estabelecimento da sessão (BR-8).
 *
 * <p>Mesma mensagem para "id inexistente" e para "id de outro tenant" — de
 * propósito. Revelar que um id específico pertence a outro estabelecimento
 * entregaria informação sobre a base de clientes de outra empresa a partir de
 * um POST forjado (ver Security na spec técnica de cadastro-servico-oferta).
 */
public class ProfessionalNotFoundException extends DomainException {

    public ProfessionalNotFoundException() {
        super("Profissional não encontrado.", "professionalId");
    }
}
