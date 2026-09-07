package com.agendaia.catalog.application;

import com.agendaia.catalog.api.ActiveOfferingRef;
import com.agendaia.catalog.api.PublicOfferingRef;
import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.catalog.api.ServiceOfferingRef;
import com.agendaia.catalog.application.port.out.ServiceOfferingRepository;
import com.agendaia.catalog.application.port.out.ServiceRepository;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.organization.api.ProfessionalRef;
import com.agendaia.platform.tenant.TenantContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação de {@link ServiceOfferingDirectory} — a única classe de
 * {@code catalog} que outro contexto enxerga através da interface.
 *
 * <p>Tenant lido do {@link TenantContext}, nunca de argumento (DD-1).
 */
@Service
public class ServiceOfferingDirectoryHandler implements ServiceOfferingDirectory {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ServiceRepository serviceRepository;
    private final ProfessionalDirectory professionalDirectory;

    public ServiceOfferingDirectoryHandler(
            ServiceOfferingRepository serviceOfferingRepository,
            ServiceRepository serviceRepository,
            ProfessionalDirectory professionalDirectory) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.serviceRepository = serviceRepository;
        this.professionalDirectory = professionalDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceOfferingRef> find(UUID serviceOfferingId) {
        var tenantId = TenantContext.require();

        return serviceOfferingRepository
                .findByTenantIdAndIdAndActiveTrue(tenantId.value(), serviceOfferingId)
                .map(oferta -> new ServiceOfferingRef(
                        oferta.id(),
                        oferta.professionalId(),
                        oferta.durationMinutes(),
                        oferta.bufferMinutes(),
                        serviceRepository.findById(oferta.serviceId()).map(servico -> servico.name()).orElse(null),
                        oferta.price()));
    }

    /**
     * {@link ProfessionalDirectory#listActive()} é chamado uma vez só,
     * nunca em laço por oferta (PATTERNS.md, "API entre contextos é
     * grossa") — mesma técnica de {@code ListServiceOfferingsHandler}
     * (TODO-003).
     */
    @Override
    @Transactional(readOnly = true)
    public List<PublicOfferingRef> listActiveByService(UUID serviceId) {
        var tenantId = TenantContext.require();

        var ofertas = serviceOfferingRepository.findByTenantIdAndServiceIdAndActiveTrueOrderByCreatedAtAsc(
                tenantId.value(), serviceId);

        var nomesDeProfissional = professionalDirectory.listActive().stream()
                .collect(Collectors.toMap(ProfessionalRef::id, ProfessionalRef::name));

        return ofertas.stream()
                .map(oferta -> new PublicOfferingRef(
                        oferta.id(),
                        oferta.professionalId(),
                        nomesDeProfissional.get(oferta.professionalId()),
                        oferta.durationMinutes(),
                        oferta.price().format()))
                .toList();
    }

    /**
     * {@link ProfessionalDirectory#listActive()} e
     * {@link ServiceRepository#findByTenantIdAndActiveTrueOrderByNameAsc} são
     * chamados uma vez só, nunca em laço por oferta (mesma técnica de
     * {@link #listActiveByService}).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ActiveOfferingRef> listActive() {
        var tenantId = TenantContext.require();

        var ofertas = serviceOfferingRepository.findByTenantIdAndActiveTrueOrderByCreatedAtAsc(tenantId.value());

        var nomesDeProfissional = professionalDirectory.listActive().stream()
                .collect(Collectors.toMap(ProfessionalRef::id, ProfessionalRef::name));

        var nomesDeServico = serviceRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenantId.value()).stream()
                .collect(Collectors.toMap(servico -> servico.id(), servico -> servico.name()));

        return ofertas.stream()
                .map(oferta -> new ActiveOfferingRef(
                        oferta.id(),
                        oferta.professionalId(),
                        nomesDeProfissional.get(oferta.professionalId()),
                        nomesDeServico.get(oferta.serviceId()),
                        oferta.durationMinutes(),
                        oferta.price().format()))
                .toList();
    }
}
