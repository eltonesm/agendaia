package com.agendaia.organization.application;

import com.agendaia.organization.application.port.in.BusinessOperatingHoursView;
import com.agendaia.organization.application.port.in.ListBusinessOperatingHoursUseCase;
import com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository;
import com.agendaia.platform.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista o horário de funcionamento do estabelecimento da sessão.
 *
 * <p>Tenant lido do {@link TenantContext}, nunca de argumento (DD-1).
 */
@Service
public class ListBusinessOperatingHoursHandler implements ListBusinessOperatingHoursUseCase {

    private final BusinessOperatingHoursRepository businessOperatingHoursRepository;

    public ListBusinessOperatingHoursHandler(BusinessOperatingHoursRepository businessOperatingHoursRepository) {
        this.businessOperatingHoursRepository = businessOperatingHoursRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessOperatingHoursView> list() {
        var tenantId = TenantContext.require();

        return businessOperatingHoursRepository
                .findByTenantIdAndActiveTrueOrderByDayOfWeekAscOpensAtAsc(tenantId.value())
                .stream()
                .map(faixa -> new BusinessOperatingHoursView(faixa.id(), faixa.dayOfWeek(), faixa.opensAt(), faixa.closesAt()))
                .toList();
    }
}
