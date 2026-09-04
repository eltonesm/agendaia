package com.agendaia.catalog.application;

import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.catalog.api.ServiceOfferingRef;
import com.agendaia.catalog.application.port.out.ServiceOfferingRepository;
import com.agendaia.platform.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;
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

    public ServiceOfferingDirectoryHandler(ServiceOfferingRepository serviceOfferingRepository) {
        this.serviceOfferingRepository = serviceOfferingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceOfferingRef> find(UUID serviceOfferingId) {
        var tenantId = TenantContext.require();

        return serviceOfferingRepository
                .findByTenantIdAndIdAndActiveTrue(tenantId.value(), serviceOfferingId)
                .map(oferta -> new ServiceOfferingRef(
                        oferta.id(), oferta.professionalId(), oferta.durationMinutes(), oferta.bufferMinutes()));
    }
}
