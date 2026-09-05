package com.agendaia.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.catalog.api.ServiceOfferingRef;
import com.agendaia.customer.api.CustomerDirectory;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.in.BookAppointmentCommand;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.exception.PhoneAppointmentLimitExceededException;
import com.agendaia.scheduling.domain.exception.ServiceOfferingNotFoundException;
import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Orquestração isolada, sem Spring e sem banco — catalog.api, customer.api e a porta de saída mockados. */
@ExtendWith(MockitoExtension.class)
class BookAppointmentHandlerTest {

    @Mock private ServiceOfferingDirectory serviceOfferingDirectory;
    @Mock private CustomerDirectory customerDirectory;
    @Mock private AppointmentRepository appointmentRepository;

    private BookAppointmentHandler handler;

    private final TenantId tenant = TenantId.of(UuidV7.generate());
    private final UUID offeringId = UuidV7.generate();
    private final UUID professionalId = UuidV7.generate();
    private final UUID customerId = UuidV7.generate();
    private final Instant startsAt = Instant.parse("2026-09-07T11:00:00Z");

    @BeforeEach
    void montar() {
        handler = new BookAppointmentHandler(serviceOfferingDirectory, customerDirectory, appointmentRepository);
        TenantContext.set(tenant);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private ServiceOfferingRef ofertaValida() {
        return new ServiceOfferingRef(offeringId, professionalId, 30, 0, "Corte de Cabelo", new Money(3000));
    }

    private BookAppointmentCommand comando() {
        return new BookAppointmentCommand(offeringId, startsAt, "Joao da Silva", "11988887777");
    }

    @Test
    @DisplayName("caminho feliz: resolve customer, checa teto, monta o retrato e grava (BR-2)")
    void caminhoFeliz() {
        when(serviceOfferingDirectory.find(offeringId)).thenReturn(Optional.of(ofertaValida()));
        when(customerDirectory.findOrCreate("Joao da Silva", "11988887777")).thenReturn(customerId);
        when(appointmentRepository.countFutureActive(any(), any(), any())).thenReturn(0L);
        when(appointmentRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        var resultado = handler.handle(comando());

        assertThat(resultado.serviceName()).isEqualTo("Corte de Cabelo");

        var captor = ArgumentCaptor.forClass(com.agendaia.scheduling.domain.Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        var salvo = captor.getValue();
        assertThat(salvo.tenantId()).isEqualTo(tenant);
        assertThat(salvo.professionalId()).isEqualTo(professionalId);
        assertThat(salvo.customerId()).isEqualTo(customerId);
        assertThat(salvo.serviceName()).isEqualTo("Corte de Cabelo");
        assertThat(salvo.durationMinutes()).isEqualTo(30);
        assertThat(salvo.price()).isEqualTo(new Money(3000));
        assertThat(salvo.startsAt()).isEqualTo(startsAt);
        assertThat(salvo.endsAt()).isEqualTo(startsAt.plusSeconds(1800));
    }

    @Test
    @DisplayName("oferta não encontrada (id inexistente ou de outro tenant) recusa antes de chamar customer ou gravar (BR-5)")
    void ofertaNaoEncontradaRecusaAntes() {
        when(serviceOfferingDirectory.find(offeringId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(comando())).isInstanceOf(ServiceOfferingNotFoundException.class);

        verify(customerDirectory, never()).findOrCreate(any(), any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("telefone com 3 agendamentos futuros ativos recusa o 4o, sem gravar (BR-9)")
    void teleFoneNoTetoRecusaSemGravar() {
        when(serviceOfferingDirectory.find(offeringId)).thenReturn(Optional.of(ofertaValida()));
        when(customerDirectory.findOrCreate(any(), any())).thenReturn(customerId);
        when(appointmentRepository.countFutureActive(any(), any(), any())).thenReturn(3L);

        assertThatThrownBy(() -> handler.handle(comando()))
                .isInstanceOf(PhoneAppointmentLimitExceededException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("telefone com 2 agendamentos futuros ainda pode marcar o 3o (limite não é exclusivo)")
    void telefoneComDoisAindaPodeMarcarOTerceiro() {
        when(serviceOfferingDirectory.find(offeringId)).thenReturn(Optional.of(ofertaValida()));
        when(customerDirectory.findOrCreate(any(), any())).thenReturn(customerId);
        when(appointmentRepository.countFutureActive(any(), any(), any())).thenReturn(2L);
        when(appointmentRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        handler.handle(comando());

        verify(appointmentRepository).save(any());
    }

    @Test
    @DisplayName("reaproveita o mesmo Customer resolvido por CustomerDirectory, nunca cria um id à parte")
    void reaproveitaOCustomerResolvido() {
        when(serviceOfferingDirectory.find(offeringId)).thenReturn(Optional.of(ofertaValida()));
        when(customerDirectory.findOrCreate("Joao da Silva", "11988887777")).thenReturn(customerId);
        when(appointmentRepository.countFutureActive(any(), any(), any())).thenReturn(0L);
        when(appointmentRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        handler.handle(comando());

        var captor = ArgumentCaptor.forClass(com.agendaia.scheduling.domain.Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
    }
}
