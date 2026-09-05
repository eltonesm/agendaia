package com.agendaia.catalog.application;

import com.agendaia.catalog.api.PublicServiceRef;
import com.agendaia.catalog.api.ServiceDirectory;
import com.agendaia.catalog.application.port.out.ServiceRepository;
import com.agendaia.platform.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link ServiceDirectory}. Tenant lido do {@code TenantContext}, nunca de argumento (DD-1). */
@Service
public class ServiceDirectoryHandler implements ServiceDirectory {

    private final ServiceRepository serviceRepository;

    public ServiceDirectoryHandler(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicServiceRef> listActive() {
        var tenantId = TenantContext.require();

        return serviceRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenantId.value()).stream()
                .map(servico -> new PublicServiceRef(servico.id(), servico.name()))
                .toList();
    }
}
