package com.agendaia.organization.application;

import com.agendaia.organization.application.command.RegisterProfessionalCommand;
import com.agendaia.organization.application.port.in.RegisterProfessionalUseCase;
import com.agendaia.organization.application.port.in.RegisteredProfessional;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.ProfessionalRepository;
import com.agendaia.platform.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastra um profissional do estabelecimento da sessão.
 *
 * <p>Lê o tenant do {@link TenantContext}, nunca de argumento (DD-1) — mesmo
 * padrão do {@code ViewDashboardHandler} da TODO-001. Sem verificação de
 * corrida a fazer: ao contrário do slug de {@code Business}, não há restrição
 * de unicidade em {@code professional.name} (DD-3), então não existe conflito
 * a traduzir.
 */
@Service
public class RegisterProfessionalHandler implements RegisterProfessionalUseCase {

    private final ProfessionalRepository professionalRepository;

    public RegisterProfessionalHandler(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    @Override
    @Transactional
    public RegisteredProfessional register(RegisterProfessionalCommand command) {
        var tenantId = TenantContext.require();

        var profissional = Professional.register(tenantId, command.name());
        professionalRepository.saveAndFlush(profissional);

        return new RegisteredProfessional(profissional.id(), profissional.name());
    }
}
