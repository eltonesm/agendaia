package com.agendaia.catalog.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * Já existe um serviço com este nome no estabelecimento (BR-1).
 *
 * <p>Ao contrário de {@code Professional.name} (rótulo, duplicata aceitável),
 * {@code Service} é entrada de catálogo — dois "Corte de Cabelo" no mesmo
 * estabelecimento confundiriam o cliente na tela pública.
 */
public class ServiceNameAlreadyUsedException extends DomainException {

    public ServiceNameAlreadyUsedException() {
        super("Já existe um serviço com este nome.", "name");
    }
}
