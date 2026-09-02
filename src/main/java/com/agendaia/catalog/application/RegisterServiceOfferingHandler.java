package com.agendaia.catalog.application;

import com.agendaia.catalog.application.command.RegisterServiceOfferingCommand;
import com.agendaia.catalog.application.port.in.RegisterServiceOfferingUseCase;
import com.agendaia.catalog.application.port.in.RegisteredServiceOffering;
import com.agendaia.catalog.domain.ServiceOffering;
import com.agendaia.catalog.domain.ServiceOfferingRepository;
import com.agendaia.catalog.domain.ServiceRepository;
import com.agendaia.catalog.domain.exception.ProfessionalNotFoundException;
import com.agendaia.catalog.domain.exception.ServiceNotFoundException;
import com.agendaia.catalog.domain.exception.ServiceOfferingAlreadyExistsException;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.platform.tenant.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastra uma oferta do estabelecimento da sessão.
 *
 * <p>Único caso de uso do projeto que atravessa contexto: chama
 * {@link ProfessionalDirectory#listActive()} — o pacote {@code api} de
 * {@code organization} — para confirmar que o profissional recebido
 * pertence ao tenant da sessão, <strong>antes</strong> de gravar (DD-1,
 * BR-8). Não há chave estrangeira que garanta isso no banco (DD-2): a
 * garantia é de aplicação, e precisa estar realmente aqui.
 */
@Service
public class RegisterServiceOfferingHandler implements RegisterServiceOfferingUseCase {

    private static final String CONSTRAINT_UNIQUE = "service_offering_unique";

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ServiceRepository serviceRepository;
    private final ProfessionalDirectory professionalDirectory;

    public RegisterServiceOfferingHandler(
            ServiceOfferingRepository serviceOfferingRepository,
            ServiceRepository serviceRepository,
            ProfessionalDirectory professionalDirectory) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.serviceRepository = serviceRepository;
        this.professionalDirectory = professionalDirectory;
    }

    @Override
    @Transactional
    public RegisteredServiceOffering register(RegisterServiceOfferingCommand command) {
        var tenantId = TenantContext.require();

        if (!serviceRepository.existsByIdAndTenantId(command.serviceId(), tenantId.value())) {
            throw new ServiceNotFoundException();
        }

        var profissionalValido = professionalDirectory.listActive().stream()
                .anyMatch(profissional -> profissional.id().equals(command.professionalId()));
        if (!profissionalValido) {
            throw new ProfessionalNotFoundException();
        }

        if (serviceOfferingRepository.existsByTenantIdAndServiceIdAndProfessionalId(
                tenantId.value(), command.serviceId(), command.professionalId())) {
            throw new ServiceOfferingAlreadyExistsException();
        }

        var oferta = ServiceOffering.register(
                tenantId,
                command.serviceId(),
                command.professionalId(),
                command.durationMinutes(),
                command.price(),
                command.bufferMinutes());

        try {
            serviceOfferingRepository.saveAndFlush(oferta);
        } catch (DataIntegrityViolationException e) {
            throw traduzir(e);
        }

        return new RegisteredServiceOffering(oferta.id());
    }

    private RuntimeException traduzir(DataIntegrityViolationException e) {
        var causa = e.getMostSpecificCause().getMessage();
        if (causa != null && causa.contains(CONSTRAINT_UNIQUE)) {
            return new ServiceOfferingAlreadyExistsException();
        }
        return e;
    }
}
