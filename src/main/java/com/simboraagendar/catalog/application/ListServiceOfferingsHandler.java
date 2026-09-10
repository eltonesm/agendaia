package com.simboraagendar.catalog.application;

import com.simboraagendar.catalog.application.port.in.ListServiceOfferingsUseCase;
import com.simboraagendar.catalog.application.port.in.ServiceOfferingView;
import com.simboraagendar.catalog.application.port.out.ServiceOfferingRepository;
import com.simboraagendar.catalog.application.port.out.ServiceRepository;
import com.simboraagendar.catalog.domain.Service;
import com.simboraagendar.catalog.domain.ServiceOffering;
import com.simboraagendar.organization.api.ProfessionalDirectory;
import com.simboraagendar.organization.api.ProfessionalRef;
import com.simboraagendar.platform.tenant.TenantContext;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista as ofertas do estabelecimento da sessão, com nomes já resolvidos.
 *
 * <p>{@link ProfessionalDirectory#listActive()} é chamado uma vez só, nunca
 * em laço por oferta — mesma exigência de {@code PATTERNS.md} ("API entre
 * contextos é grossa, nunca conversadeira").
 */
@org.springframework.stereotype.Service
public class ListServiceOfferingsHandler implements ListServiceOfferingsUseCase {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ServiceRepository serviceRepository;
    private final ProfessionalDirectory professionalDirectory;

    public ListServiceOfferingsHandler(
            ServiceOfferingRepository serviceOfferingRepository,
            ServiceRepository serviceRepository,
            ProfessionalDirectory professionalDirectory) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.serviceRepository = serviceRepository;
        this.professionalDirectory = professionalDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceOfferingView> list() {
        var tenantId = TenantContext.require();

        var ofertas = serviceOfferingRepository.findByTenantIdAndActiveTrueOrderByCreatedAtAsc(tenantId.value());

        var idsDeServico = ofertas.stream().map(ServiceOffering::serviceId).distinct().toList();
        var nomesDeServico =
                serviceRepository.findAllById(idsDeServico).stream().collect(Collectors.toMap(Service::id, Service::name));

        var nomesDeProfissional = professionalDirectory.listActive().stream()
                .collect(Collectors.toMap(ProfessionalRef::id, ProfessionalRef::name));

        return ofertas.stream()
                .map(oferta -> new ServiceOfferingView(
                        oferta.id(),
                        nomesDeServico.get(oferta.serviceId()),
                        nomesDeProfissional.get(oferta.professionalId()),
                        oferta.durationMinutes(),
                        oferta.price().format()))
                .toList();
    }
}
