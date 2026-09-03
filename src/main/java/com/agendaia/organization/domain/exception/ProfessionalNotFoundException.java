package com.agendaia.organization.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * O profissional informado não está entre os profissionais ativos do
 * estabelecimento da sessão (BR-8).
 *
 * <p>Mesma mensagem para "id inexistente" e para "id de outro tenant" — de
 * propósito, mesmo raciocínio da TODO-003. Aqui a checagem é uma consulta
 * direta ao {@code ProfessionalRepository} (mesmo contexto), não uma chamada
 * a outro contexto.
 */
public class ProfessionalNotFoundException extends DomainException {

    public ProfessionalNotFoundException() {
        super("Profissional não encontrado.", "professionalId");
    }
}
