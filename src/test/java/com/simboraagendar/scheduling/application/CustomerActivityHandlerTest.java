package com.simboraagendar.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.simboraagendar.customer.api.CustomerDirectory;
import com.simboraagendar.customer.api.CustomerRef;
import com.simboraagendar.customer.api.PagedCustomers;
import com.simboraagendar.scheduling.application.port.out.AppointmentRepository;
import com.simboraagendar.scheduling.application.port.out.CustomerActivity;
import com.simboraagendar.scheduling.domain.Appointment;
import com.simboraagendar.scheduling.domain.AppointmentStatus;
import com.simboraagendar.scheduling.domain.PaymentStatus;
import com.simboraagendar.shared.Money;
import com.simboraagendar.shared.TenantId;
import com.simboraagendar.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Orquestração isolada, sem Spring e sem banco — as duas portas de
 * CustomerActivityHandler (gestao-de-clientes, DD-1/DD-2/DD-4).
 */
@ExtendWith(MockitoExtension.class)
class CustomerActivityHandlerTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private CustomerDirectory customerDirectory;

    private CustomerActivityHandler handler;

    private final TenantId tenant = TenantId.of(UuidV7.generate());
    private final UUID clienteComVisitaId = UuidV7.generate();
    private final UUID clienteNovoId = UuidV7.generate();

    @BeforeEach
    void montar() {
        handler = new CustomerActivityHandler(appointmentRepository, customerDirectory);
        com.simboraagendar.platform.tenant.TenantContext.set(tenant);
    }

    @AfterEach
    void limparContexto() {
        com.simboraagendar.platform.tenant.TenantContext.clear();
    }

    @Test
    @DisplayName("list: cliente ausente do mapa de atividade aparece com visitCount=0 e isNew=true (BR-1)")
    void listMarcaComoNovoQuandoAusenteDoMapa() {
        when(customerDirectory.listForTenant(0, 20))
                .thenReturn(new PagedCustomers(
                        List.of(
                                new CustomerRef(clienteComVisitaId, "Maria", "+5511999990000"),
                                new CustomerRef(clienteNovoId, "João", "+5511999990001")),
                        0,
                        20,
                        2));
        when(appointmentRepository.findActivityByCustomerIds(any(), anyCollection()))
                .thenReturn(Map.of(clienteComVisitaId, new CustomerActivity(3, new Money(9000), new Money(0))));

        var pagina = handler.list(0, 20);

        var maria = pagina.items().stream()
                .filter(c -> c.customerId().equals(clienteComVisitaId))
                .findFirst()
                .orElseThrow();
        var joao = pagina.items().stream()
                .filter(c -> c.customerId().equals(clienteNovoId))
                .findFirst()
                .orElseThrow();

        assertThat(maria.visitCount()).isEqualTo(3);
        assertThat(maria.isNew()).isFalse();
        assertThat(joao.visitCount()).isEqualTo(0);
        assertThat(joao.isNew()).isTrue();
    }

    @Test
    @DisplayName("list: propaga paginacao da consulta de clientes")
    void listPropagaPaginacao() {
        when(customerDirectory.listForTenant(1, 20)).thenReturn(new PagedCustomers(List.of(), 1, 20, 45));
        when(appointmentRepository.findActivityByCustomerIds(any(), anyCollection())).thenReturn(Map.of());

        var pagina = handler.list(1, 20);

        assertThat(pagina.page()).isEqualTo(1);
        assertThat(pagina.size()).isEqualTo(20);
        assertThat(pagina.totalElements()).isEqualTo(45);
    }

    @Test
    @DisplayName("detail: soma visitas, gasto e em aberto a partir do historico COMPLETED (BR-7/BR-8/BR-9)")
    void detailSomaTotaisCorretamente() {
        when(customerDirectory.find(clienteComVisitaId))
                .thenReturn(Optional.of(new CustomerRef(clienteComVisitaId, "Maria", "+5511999990000")));
        when(appointmentRepository.findCompletedByTenantIdAndCustomerId(tenant, clienteComVisitaId))
                .thenReturn(List.of(
                        visitaCompleta(new Money(5000), PaymentStatus.PAID),
                        visitaCompleta(new Money(3000), PaymentStatus.ON_CREDIT)));

        var detalhe = handler.detail(clienteComVisitaId).orElseThrow();

        assertThat(detalhe.visitCount()).isEqualTo(2);
        assertThat(detalhe.totalSpent()).isEqualTo(new Money(8000));
        assertThat(detalhe.totalOwed()).isEqualTo(new Money(3000));
        assertThat(detalhe.visits()).hasSize(2);
    }

    @Test
    @DisplayName("detail: cliente de outro tenant (ou inexistente) devolve Optional.empty()")
    void detailTenantErradoDevolveVazio() {
        when(customerDirectory.find(clienteComVisitaId)).thenReturn(Optional.empty());

        assertThat(handler.detail(clienteComVisitaId)).isEmpty();
    }

    private Appointment visitaCompleta(Money preco, PaymentStatus paymentStatus) {
        var inicio = Instant.now().minus(1, ChronoUnit.DAYS);
        return Appointment.reconstitute(
                UuidV7.generate(),
                tenant,
                UuidV7.generate(),
                UuidV7.generate(),
                clienteComVisitaId,
                AppointmentStatus.COMPLETED,
                inicio,
                inicio.plus(30, ChronoUnit.MINUTES),
                "Corte de Cabelo",
                30,
                preco,
                paymentStatus);
    }
}
