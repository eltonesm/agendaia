package com.agendaia.catalog.application;

import com.agendaia.catalog.application.command.RegisterServiceCommand;
import com.agendaia.catalog.application.port.in.RegisterServiceUseCase;
import com.agendaia.catalog.application.port.in.RegisteredService;
import com.agendaia.catalog.domain.Service;
import com.agendaia.catalog.domain.ServiceRepository;
import com.agendaia.catalog.domain.exception.ServiceNameAlreadyUsedException;
import com.agendaia.platform.tenant.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastra um serviço do estabelecimento da sessão.
 *
 * <p>Lê o tenant do {@link TenantContext}, nunca de argumento (DD-1). Nome é
 * único por tenant (BR-1): verificação antecipada dá erro no campo certo, e o
 * {@code CONSTRAINT service_name_unique} do banco cobre a corrida entre duas
 * requisições simultâneas — mesmo padrão de {@code RegisterBusinessHandler}.
 */
@org.springframework.stereotype.Service
public class RegisterServiceHandler implements RegisterServiceUseCase {

    private static final String CONSTRAINT_NAME = "service_name_unique";

    private final ServiceRepository serviceRepository;

    public RegisterServiceHandler(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    @Transactional
    public RegisteredService register(RegisterServiceCommand command) {
        var tenantId = TenantContext.require();
        var nome = command.name() == null ? "" : command.name().strip();

        if (serviceRepository.existsByTenantIdAndName(tenantId.value(), nome)) {
            throw new ServiceNameAlreadyUsedException();
        }

        var servico = Service.register(tenantId, command.name(), command.description());

        try {
            serviceRepository.saveAndFlush(servico);
        } catch (DataIntegrityViolationException e) {
            throw traduzir(e);
        }

        return new RegisteredService(servico.id(), servico.name());
    }

    private RuntimeException traduzir(DataIntegrityViolationException e) {
        var causa = e.getMostSpecificCause().getMessage();
        if (causa != null && causa.contains(CONSTRAINT_NAME)) {
            return new ServiceNameAlreadyUsedException();
        }
        return e;
    }
}
