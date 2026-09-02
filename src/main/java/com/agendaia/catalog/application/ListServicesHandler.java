package com.agendaia.catalog.application;

import com.agendaia.catalog.application.port.in.ListServicesUseCase;
import com.agendaia.catalog.application.port.in.ServiceView;
import com.agendaia.catalog.domain.ServiceRepository;
import com.agendaia.platform.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista os serviços do estabelecimento da sessão.
 *
 * <p>Tenant lido do {@link TenantContext}, nunca de argumento (DD-1).
 */
@Service
public class ListServicesHandler implements ListServicesUseCase {

    private final ServiceRepository serviceRepository;

    public ListServicesHandler(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceView> list() {
        var tenantId = TenantContext.require();

        return serviceRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenantId.value()).stream()
                .map(servico -> new ServiceView(servico.id(), servico.name()))
                .toList();
    }
}
