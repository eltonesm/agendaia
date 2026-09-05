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
 *
 * <p>Uma violação da constraint (duas requisições com o mesmo telefone
 * nunca visto antes, no mesmo instante) propositalmente não é recapturada
 * aqui: {@code findOrCreate} roda na mesma transação de {@code
 * BookAppointmentHandler.handle} (propagação REQUIRED), e o Postgres aborta
 * a transação inteira na violação — qualquer novo comando na mesma conexão
 * falharia de novo, agora com "current transaction is aborted", mais confuso
 * que a exceção original. Cai no {@code GlobalExceptionHandler} genérico;
 * caso raro o bastante (mesmo telefone, mesmo instante, primeira vez) para
 * não justificar transação separada só para esse retry (DEBT candidato).
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
