package com.agendaia.organization.application;

import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.organization.api.ProfessionalRef;
import com.agendaia.organization.domain.ProfessionalRepository;
import com.agendaia.platform.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação de {@link ProfessionalDirectory} — a única classe de
 * {@code organization} que outro contexto enxerga através da interface.
 *
 * <p>Tenant lido do {@link TenantContext}, nunca de argumento (DD-1).
 */
@Service
public class ProfessionalDirectoryHandler implements ProfessionalDirectory {

    private final ProfessionalRepository professionalRepository;

    public ProfessionalDirectoryHandler(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalRef> listActive() {
        var tenantId = TenantContext.require();

        return professionalRepository
                .findByTenantIdAndActiveTrueOrderByNameAsc(tenantId.value())
                .stream()
                .map(profissional -> new ProfessionalRef(profissional.id(), profissional.name()))
                .toList();
    }
}
