package com.agendaia.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.catalog.api.ServiceOfferingRef;
import com.agendaia.customer.api.CustomerDirectory;
import com.agendaia.customer.api.CustomerRef;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.organization.api.ProfessionalRef;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.in.BookAppointmentCommand;
import com.agendaia.scheduling.application.port.in.RescheduleAppointmentCommand;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.exception.AppointmentNotFoundException;
import com.agendaia.scheduling.domain.exception.SlotUnavailableException;
import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Orquestração isolada, sem Spring e sem banco — as quatro portas de ProfessionalAgendaHandler. */
@ExtendWith(MockitoExtension.class)
class ProfessionalAgendaHandlerTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ServiceOfferingDirectory serviceOfferingDirectory;
    @Mock private CustomerDirectory customerDirectory;
    @Mock private ProfessionalDirectory professionalDirectory;

    private ProfessionalAgendaHandler handler;

    private final TenantId tenant = TenantId.of(UuidV7.generate());
    private final UUID professionalId = UuidV7.generate();
    private final UUID customerId = UuidV7.generate();
    private final UUID appointmentId = UuidV7.generate();
    private final UUID serviceOfferingId = UuidV7.generate();
    private final Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);

    @BeforeEach
    void montar() {
        handler = new ProfessionalAgendaHandler(
                appointmentRepository, serviceOfferingDirectory, customerDirectory, professionalDirectory);
        TenantContext.set(tenant);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private Appointment agendamento(AppointmentStatus status, Instant inicio) {
        return Appointment.reconstitute(
                appointmentId,
                tenant,
                professionalId,
                serviceOfferingId,
                customerId,
                status,
                inicio,
                inicio.plus(30, ChronoUnit.MINUTES),
                "Corte de Cabelo",
                30,
                new Money(3000));
    }

    private ServiceOfferingRef oferta() {
        return new ServiceOfferingRef(serviceOfferingId, professionalId, 30, 0, "Corte de Cabelo", new Money(3000));
    }

    // --- ViewAgendaUseCase ---------------------------------------------

    @Test
    @DisplayName("handle: professionalId de outro tenant devolve lista vazia, nunca lança (BR-5)")
    void handleProfissionalDeOutroTenantDevolveVazio() {
        when(professionalDirectory.find(professionalId)).thenReturn(Optional.empty());

        var agenda = handler.handle(professionalId, LocalDate.now());

        assertThat(agenda).isEmpty();
    }

    @Test
    @DisplayName("handle: calcula canConfirm/canCancel/canReschedule por linha, com cliente resolvido em lote")
    void handleCalculaAcoesDisponiveis() {
        when(professionalDirectory.find(professionalId))
                .thenReturn(Optional.of(new ProfessionalRef(professionalId, "Maria")));
        var agendado = agendamento(AppointmentStatus.SCHEDULED, startsAt);
        var cancelado = agendamento(AppointmentStatus.CANCELLED, startsAt.plus(1, ChronoUnit.HOURS));
        when(appointmentRepository.findByTenantIdAndProfessionalIdAndDate(tenant, professionalId, LocalDate.now()))
                .thenReturn(List.of(agendado, cancelado));
        when(customerDirectory.findByIds(Set.of(customerId)))
                .thenReturn(List.of(new CustomerRef(customerId, "João", "+5511999990000")));

        var agenda = handler.handle(professionalId, LocalDate.now());

        assertThat(agenda).hasSize(2);
        var linhaAgendada = agenda.stream().filter(e -> e.status() == AppointmentStatus.SCHEDULED).findFirst().get();
        assertThat(linhaAgendada.customerName()).isEqualTo("João");
        assertThat(linhaAgendada.customerPhone()).isEqualTo("+5511999990000");
        assertThat(linhaAgendada.canConfirm()).isTrue();
        assertThat(linhaAgendada.canCancel()).isTrue();
        assertThat(linhaAgendada.canReschedule()).isTrue();

        var linhaCancelada = agenda.stream().filter(e -> e.status() == AppointmentStatus.CANCELLED).findFirst().get();
        assertThat(linhaCancelada.canConfirm()).isFalse();
        assertThat(linhaCancelada.canCancel()).isFalse();
        assertThat(linhaCancelada.canReschedule()).isFalse();
    }

    // --- CreateAppointmentManuallyUseCase -------------------------------

    @Test
    @DisplayName("create: não checa teto de agendamentos futuros por telefone (BR-4)")
    void createSemTetoDeTelefone() {
        when(serviceOfferingDirectory.find(serviceOfferingId)).thenReturn(Optional.of(oferta()));
        when(customerDirectory.findOrCreate("João", "+5511999990000")).thenReturn(customerId);
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = handler.create(new BookAppointmentCommand(serviceOfferingId, startsAt, "João", "+5511999990000"));

        assertThat(resultado.serviceName()).isEqualTo("Corte de Cabelo");
        verify(appointmentRepository, never()).countFutureActive(any(), any(), any());
        verify(appointmentRepository).save(any());
    }

    // --- CancelAppointmentByOwnerUseCase --------------------------------

    @Test
    @DisplayName("cancel: cancela mesmo com startsAt no passado (BR-2)")
    void cancelIgnoraHorario() {
        var passado = Instant.now().minus(1, ChronoUnit.DAYS);
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.SCHEDULED, passado)));

        handler.cancel(appointmentId);

        verify(appointmentRepository).updateStatus(eq(tenant), eq(appointmentId), eq(AppointmentStatus.CANCELLED), any());
    }

    @Test
    @DisplayName("cancel: id de outro tenant lança AppointmentNotFoundException")
    void cancelTenantErradoLancaNaoEncontrado() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.cancel(appointmentId)).isInstanceOf(AppointmentNotFoundException.class);
    }

    // --- RescheduleAppointmentUseCase ------------------------------------

    @Test
    @DisplayName("reschedule: cria o novo antes de cancelar o antigo (DD-9)")
    void rescheduleCriaNovoAntesDeCancelarAntigo() {
        var novoServiceOfferingId = UuidV7.generate();
        var novoProfissionalId = UuidV7.generate();
        var novaOferta = new ServiceOfferingRef(
                novoServiceOfferingId, novoProfissionalId, 30, 0, "Corte de Cabelo", new Money(3000));
        var novoInicio = startsAt.plus(2, ChronoUnit.HOURS);

        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.SCHEDULED, startsAt)));
        when(serviceOfferingDirectory.find(novoServiceOfferingId)).thenReturn(Optional.of(novaOferta));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = handler.reschedule(
                new RescheduleAppointmentCommand(appointmentId, novoServiceOfferingId, novoInicio));

        assertThat(resultado.startsAt().atZone(ZoneId.systemDefault()).toInstant()).isEqualTo(novoInicio);
        verify(appointmentRepository).save(any());
        verify(appointmentRepository)
                .updateStatus(eq(tenant), eq(appointmentId), eq(AppointmentStatus.CANCELLED), any());
    }

    @Test
    @DisplayName("reschedule: se o novo horário colidir, o antigo permanece intacto (DD-9)")
    void rescheduleMantemAntigoIntactoSeNovoFalhar() {
        var novoServiceOfferingId = UuidV7.generate();
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.SCHEDULED, startsAt)));
        when(serviceOfferingDirectory.find(novoServiceOfferingId)).thenReturn(Optional.of(oferta()));
        when(appointmentRepository.save(any())).thenThrow(new SlotUnavailableException());

        assertThatThrownBy(() -> handler.reschedule(
                        new RescheduleAppointmentCommand(appointmentId, novoServiceOfferingId, startsAt.plus(2, ChronoUnit.HOURS))))
                .isInstanceOf(SlotUnavailableException.class);

        verify(appointmentRepository, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("reschedule: id de outro tenant lança AppointmentNotFoundException, nunca cria o novo")
    void rescheduleTenantErradoLancaNaoEncontrado() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.reschedule(
                        new RescheduleAppointmentCommand(appointmentId, serviceOfferingId, startsAt)))
                .isInstanceOf(AppointmentNotFoundException.class);

        verify(appointmentRepository, never()).save(any());
    }
}
