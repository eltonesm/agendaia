package com.agendaia.scheduling.application;

import com.agendaia.customer.api.CustomerDirectory;
import com.agendaia.customer.api.CustomerRef;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.in.CustomerActivityDetail;
import com.agendaia.scheduling.application.port.in.CustomerActivityDetailUseCase;
import com.agendaia.scheduling.application.port.in.CustomerListEntry;
import com.agendaia.scheduling.application.port.in.ListCustomerActivityUseCase;
import com.agendaia.scheduling.application.port.in.PagedResult;
import com.agendaia.scheduling.application.port.in.VisitEntry;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.application.port.out.CustomerActivity;
import com.agendaia.shared.Money;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação de {@link ListCustomerActivityUseCase} e
 * {@link CustomerActivityDetailUseCase} (gestao-de-clientes) — mesma classe,
 * mesma funcionalidade ("atividade do cliente") sob ângulos diferentes
 * (lista vs. detalhe), mesmo desvio deliberado já documentado no
 * PATTERNS.md ("uma classe, várias portas", IDEA-017).
 *
 * <p>Mora em {@code scheduling}, não em {@code customer} nem {@code
 * organization} (DD-1 da spec técnica): {@code scheduling} já depende de
 * {@code customer.api} desde a TODO-008/TODO-110 — a direção contrária
 * fecharia um ciclo, a mesma lição da DD-6 da TODO-110.
 */
@Service
public class CustomerActivityHandler implements ListCustomerActivityUseCase, CustomerActivityDetailUseCase {

    private static final CustomerActivity SEM_ATIVIDADE = new CustomerActivity(0, new Money(0), new Money(0));

    private final AppointmentRepository appointmentRepository;
    private final CustomerDirectory customerDirectory;

    public CustomerActivityHandler(AppointmentRepository appointmentRepository, CustomerDirectory customerDirectory) {
        this.appointmentRepository = appointmentRepository;
        this.customerDirectory = customerDirectory;
    }

    /**
     * Duas consultas: clientes da página (DD-2) e, só para esses ids,
     * atividade agregada em lote — nunca uma consulta por cliente.
     */
    @Override
    @Transactional(readOnly = true)
    public PagedResult<CustomerListEntry> list(int page, int size) {
        var clientes = customerDirectory.listForTenant(page, size);
        var tenantId = TenantContext.require();

        var idsDeClientes = clientes.items().stream().map(CustomerRef::id).toList();
        var atividadePorCliente = appointmentRepository.findActivityByCustomerIds(tenantId, idsDeClientes);

        var itens = clientes.items().stream()
                .map(cliente -> paraCustomerListEntry(cliente, atividadePorCliente))
                .toList();
        return new PagedResult<>(itens, clientes.page(), clientes.size(), clientes.totalElements());
    }

    private static CustomerListEntry paraCustomerListEntry(
            CustomerRef cliente, Map<UUID, CustomerActivity> atividadePorCliente) {
        var atividade = atividadePorCliente.getOrDefault(cliente.id(), SEM_ATIVIDADE);
        return new CustomerListEntry(
                cliente.id(), cliente.name(), cliente.phone(), atividade.visitCount(), atividade.visitCount() == 0);
    }

    /**
     * Histórico completo (uma consulta) e totais somados em memória —
     * mesmo padrão de {@code DailyScheduleSummaryHandler} (TODO-110): número
     * pequeno de visitas por cliente, sem custo relevante.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerActivityDetail> detail(UUID customerId) {
        var tenantId = TenantContext.require();
        var cliente = customerDirectory.find(customerId);
        if (cliente.isEmpty()) {
            return Optional.empty();
        }

        var visitasCompletas = appointmentRepository.findCompletedByTenantIdAndCustomerId(tenantId, customerId);

        var totalVisitas = visitasCompletas.size();
        var totalGastoCentavos =
                visitasCompletas.stream().mapToLong(a -> a.price().cents()).sum();
        var totalEmAbertoCentavos = visitasCompletas.stream()
                .filter(a -> a.paymentStatus() == com.agendaia.scheduling.domain.PaymentStatus.ON_CREDIT)
                .mapToLong(a -> a.price().cents())
                .sum();

        var visitas = visitasCompletas.stream()
                .map(a -> new VisitEntry(a.startsAt(), a.serviceName(), a.price(), a.paymentStatus()))
                .toList();

        return Optional.of(new CustomerActivityDetail(
                cliente.get().id(),
                cliente.get().name(),
                cliente.get().phone(),
                totalVisitas,
                new Money(totalGastoCentavos),
                new Money(totalEmAbertoCentavos),
                visitas));
    }
}
