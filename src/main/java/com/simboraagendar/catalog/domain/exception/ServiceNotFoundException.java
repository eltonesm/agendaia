package com.simboraagendar.catalog.domain.exception;

import com.simboraagendar.shared.DomainException;

/**
 * O serviço informado não existe, ou não pertence ao tenant da sessão.
 *
 * <p>Mesma mensagem para os dois casos, mesmo raciocínio de
 * {@link ProfessionalNotFoundException}.
 */
public class ServiceNotFoundException extends DomainException {

    public ServiceNotFoundException() {
        super("Serviço não encontrado.", "serviceId");
    }
}
