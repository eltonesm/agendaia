package com.agendaia.organization.application;

import com.agendaia.organization.api.BusinessDirectory;
import com.agendaia.organization.api.BusinessRef;
import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.domain.Business;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação de {@link BusinessDirectory}.
 *
 * <p>Deliberadamente sem {@code TenantContext.require()} nos dois métodos
 * — cada um recebe o escopo (tenant único, ou nenhum) como decisão do
 * chamador, não desta classe (DD-5 da spec técnica de
 * back-office-operador).
 */
@Service
public class BusinessDirectoryHandler implements BusinessDirectory {

    private final BusinessRepository businessRepository;

    public BusinessDirectoryHandler(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessRef> listAll() {
        return businessRepository.findAllByOrderByCreatedAtAsc().stream().map(this::toRef).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessRef> find(UUID tenantId) {
        return businessRepository.findById(tenantId).map(this::toRef);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessRef> findBySlug(String slug) {
        return businessRepository.findBySlug(slug).map(this::toRef);
    }

    private BusinessRef toRef(Business business) {
        return new BusinessRef(business.tenantId().value(), business.name(), business.slug(), business.createdAt());
    }
}
