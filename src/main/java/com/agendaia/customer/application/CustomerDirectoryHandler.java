package com.agendaia.customer.application;

import com.agendaia.customer.api.CustomerDirectory;
import com.agendaia.customer.application.port.out.CustomerRepository;
import com.agendaia.customer.domain.Customer;
import com.agendaia.platform.tenant.TenantContext;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação de {@link CustomerDirectory}. Get-or-create pelo par
 * (tenant, telefone) — a garantia de não duplicar sob concorrência é do
 * banco ({@code UNIQUE(tenant_id, phone)}), esta classe só evita a ida ao
 * banco quando já sabe que o cliente existe.
 */
@Service
public class CustomerDirectoryHandler implements CustomerDirectory {

    private final CustomerRepository customerRepository;

    public CustomerDirectoryHandler(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public UUID findOrCreate(String name, String phone) {
        var tenantId = TenantContext.require();
        var telefoneNormalizado = Customer.normalizePhone(phone);

        var existente = customerRepository.findByTenantIdAndPhone(tenantId.value(), telefoneNormalizado);
        if (existente.isPresent()) {
            var cliente = existente.get();
            cliente.renameTo(name);
            return cliente.id();
        }

        var novo = Customer.register(tenantId, name, telefoneNormalizado);
        return customerRepository.saveAndFlush(novo).id();
    }
}
