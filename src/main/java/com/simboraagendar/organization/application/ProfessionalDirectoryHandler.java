package com.simboraagendar.organization.application;

import com.simboraagendar.organization.api.ProfessionalDirectory;
import com.simboraagendar.organization.api.ProfessionalRef;
import com.simboraagendar.organization.application.port.out.ProfessionalRepository;
import com.simboraagendar.platform.tenant.TenantContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfessionalRef> find(UUID id) {
        var tenantId = TenantContext.require();

        return professionalRepository
                .findByTenantIdAndId(tenantId.value(), id)
                .map(profissional -> new ProfessionalRef(profissional.id(), profissional.name()));
    }
}
