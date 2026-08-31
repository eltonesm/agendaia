package com.agendaia.organization.application;

import com.agendaia.organization.application.port.in.ListProfessionalsUseCase;
import com.agendaia.organization.application.port.in.ProfessionalView;
import com.agendaia.organization.domain.ProfessionalRepository;
import com.agendaia.platform.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista os profissionais do estabelecimento da sessão.
 *
 * <p>Tenant lido do {@link TenantContext}, nunca de argumento (DD-1).
 */
@Service
public class ListProfessionalsHandler implements ListProfessionalsUseCase {

    private final ProfessionalRepository professionalRepository;

    public ListProfessionalsHandler(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalView> list() {
        var tenantId = TenantContext.require();

        return professionalRepository
                .findByTenantIdAndActiveTrueOrderByNameAsc(tenantId.value())
                .stream()
                .map(profissional -> new ProfessionalView(profissional.id(), profissional.name()))
                .toList();
    }
}
