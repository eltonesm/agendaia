package com.agendaia.scheduling.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * A oferta informada não existe, ou existe em outro tenant (BR-7).
 *
 * <p>Mesma mensagem para os dois casos, de propósito — mesmo raciocínio de
 * {@code ProfessionalNotFoundException} (TODO-004). A checagem é
 * {@code catalog.api.ServiceOfferingDirectory.find} retornando vazio; quem
 * decide lançar esta exceção é o handler, não o directory.
 */
public class ServiceOfferingNotFoundException extends DomainException {

    public ServiceOfferingNotFoundException() {
        super("Oferta não encontrada.");
    }
}
