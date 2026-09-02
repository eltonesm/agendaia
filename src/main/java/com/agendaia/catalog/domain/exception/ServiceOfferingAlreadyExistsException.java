package com.agendaia.catalog.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * Este profissional já tem uma oferta deste serviço (BR-7).
 *
 * <p>Único por {@code (tenant, service, professional)} — um profissional tem
 * no máximo uma oferta de cada serviço.
 */
public class ServiceOfferingAlreadyExistsException extends DomainException {

    public ServiceOfferingAlreadyExistsException() {
        super("Este profissional já tem uma oferta deste serviço.", "professionalId");
    }
}
