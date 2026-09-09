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
import com.agendaia.scheduling.domain.PaymentStatus;
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
    @Mock private SchedulingMetrics schedulingMetrics;

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
                appointmentRepository,
                serviceOfferingDirectory,
                customerDirectory,
                professionalDirectory,
                schedulingMetrics);
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
                new Money(3000),
                PaymentStatus.PENDING);
    }

    private Appointment agendamentoComPagamento(PaymentStatus paymentStatus) {
        return Appointment.reconstitute(
                appointmentId,
                tenant,
                professionalId,
                serviceOfferingId,
                customerId,
                AppointmentStatus.COMPLETED,
                startsAt,
                startsAt.plus(30, ChronoUnit.MINUTES),
                "Corte de Cabelo",
                30,
                new Money(3000),
                paymentStatus);
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

    @Test
    @DisplayName("handle: COMPLETED nao mostra canCancel/canReschedule (DD-4, correcao do guard antigo que so excluia CANCELLED)")
    void handleAgendamentoConcluidoSemAcoesDeAbrirDeNovo() {
        when(professionalDirectory.find(professionalId))
                .thenReturn(Optional.of(new ProfessionalRef(professionalId, "Maria")));
        var passado = Instant.now().minus(1, ChronoUnit.HOURS);
        var concluido = agendamento(AppointmentStatus.COMPLETED, passado);
        when(appointmentRepository.findByTenantIdAndProfessionalIdAndDate(tenant, professionalId, LocalDate.now()))
                .thenReturn(List.of(concluido));
        when(customerDirectory.findByIds(Set.of(customerId)))
                .thenReturn(List.of(new CustomerRef(customerId, "João", "+5511999990000")));

        var linha = handler.handle(professionalId, LocalDate.now()).get(0);

        assertThat(linha.canConfirm()).isFalse();
        assertThat(linha.canCancel()).isFalse();
        assertThat(linha.canReschedule()).isFalse();
        assertThat(linha.canComplete()).isFalse();
    }

    @Test
    @DisplayName("handle: canComplete e verdadeiro so quando SCHEDULED/CONFIRMED e o horario ja comecou")
    void handleCanCompleteSoDepoisDoHorarioComecar() {
        when(professionalDirectory.find(professionalId))
                .thenReturn(Optional.of(new ProfessionalRef(professionalId, "Maria")));
        var jaComecou = agendamento(AppointmentStatus.SCHEDULED, Instant.now().minus(10, ChronoUnit.MINUTES));
        var aindaNaoComecou = agendamento(AppointmentStatus.CONFIRMED, startsAt);
        when(appointmentRepository.findByTenantIdAndProfessionalIdAndDate(tenant, professionalId, LocalDate.now()))
                .thenReturn(List.of(jaComecou, aindaNaoComecou));
        when(customerDirectory.findByIds(Set.of(customerId)))
                .thenReturn(List.of(new CustomerRef(customerId, "João", "+5511999990000")));

        var agenda = handler.handle(professionalId, LocalDate.now());

        var linhaJaComecou =
                agenda.stream().filter(e -> e.status() == AppointmentStatus.SCHEDULED).findFirst().get();
        assertThat(linhaJaComecou.canComplete()).isTrue();

        var linhaAindaNaoComecou =
                agenda.stream().filter(e -> e.status() == AppointmentStatus.CONFIRMED).findFirst().get();
        assertThat(linhaAindaNaoComecou.canComplete()).isFalse();
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
        verify(schedulingMetrics).appointmentCreated();
        verify(schedulingMetrics, never()).slotConflict();
    }

    @Test
    @DisplayName("create: horario colidindo incrementa a metrica de falha por conflito, nunca a de criado (TODO-108)")
    void createComColisaoIncrementaSlotConflict() {
        when(serviceOfferingDirectory.find(serviceOfferingId)).thenReturn(Optional.of(oferta()));
        when(customerDirectory.findOrCreate("João", "+5511999990000")).thenReturn(customerId);
        when(appointmentRepository.save(any())).thenThrow(new SlotUnavailableException());

        assertThatThrownBy(() -> handler.create(
                        new BookAppointmentCommand(serviceOfferingId, startsAt, "João", "+5511999990000")))
                .isInstanceOf(SlotUnavailableException.class);

        verify(schedulingMetrics).slotConflict();
        verify(schedulingMetrics, never()).appointmentCreated();
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
        verify(schedulingMetrics).appointmentCancelled();
    }

    @Test
    @DisplayName("cancel: agendamento ja COMPLETED nao reabre, nao grava nem incrementa metrica (BR-2, sistema-de-design-admin)")
    void cancelNaoReabreCompleted() {
        var passado = Instant.now().minus(1, ChronoUnit.DAYS);
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.COMPLETED, passado)));

        handler.cancel(appointmentId);

        verify(appointmentRepository, never()).updateStatus(any(), any(), any(), any());
        verify(schedulingMetrics, never()).appointmentCancelled();
    }

    @Test
    @DisplayName("cancel: id de outro tenant lança AppointmentNotFoundException")
    void cancelTenantErradoLancaNaoEncontrado() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.cancel(appointmentId)).isInstanceOf(AppointmentNotFoundException.class);
    }

    // --- CompleteAppointmentUseCase ---------------------------------------

    @Test
    @DisplayName("complete: SCHEDULED com startsAt ja passado vira COMPLETED e incrementa a metrica (BR-1/BR-3)")
    void completeTransicionaQuandoHorarioJaChegou() {
        var passado = Instant.now().minus(10, ChronoUnit.MINUTES);
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.SCHEDULED, passado)));

        handler.complete(appointmentId);

        verify(appointmentRepository).updateStatus(eq(tenant), eq(appointmentId), eq(AppointmentStatus.COMPLETED), any());
        verify(schedulingMetrics).appointmentCompleted();
    }

    @Test
    @DisplayName("complete: antes de startsAt nao transiciona nem grava (BR-3)")
    void completeAbsorveAntesDoHorario() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.SCHEDULED, startsAt)));

        handler.complete(appointmentId);

        verify(appointmentRepository, never()).updateStatus(any(), any(), any(), any());
        verify(schedulingMetrics, never()).appointmentCompleted();
    }

    @Test
    @DisplayName("complete: agendamento CANCELLED nao transiciona nem grava (BR-1)")
    void completeAbsorveSeCancelado() {
        var passado = Instant.now().minus(10, ChronoUnit.MINUTES);
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.CANCELLED, passado)));

        handler.complete(appointmentId);

        verify(appointmentRepository, never()).updateStatus(any(), any(), any(), any());
        verify(schedulingMetrics, never()).appointmentCompleted();
    }

    @Test
    @DisplayName("complete: id de outro tenant lanca AppointmentNotFoundException")
    void completeTenantErradoLancaNaoEncontrado() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.complete(appointmentId)).isInstanceOf(AppointmentNotFoundException.class);

        verify(schedulingMetrics, never()).appointmentCompleted();
    }

    // --- UpdatePaymentStatusUseCase ---------------------------------------

    @Test
    @DisplayName("updatePaymentStatus: grava quando ha mudanca real de status de pagamento (BR-2)")
    void updatePaymentStatusGravaQuandoMuda() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamentoComPagamento(PaymentStatus.PENDING)));

        handler.updatePaymentStatus(appointmentId, PaymentStatus.ON_CREDIT);

        verify(appointmentRepository)
                .updatePaymentStatus(eq(tenant), eq(appointmentId), eq(PaymentStatus.ON_CREDIT), any());
    }

    @Test
    @DisplayName("updatePaymentStatus: nao grava quando o status pedido ja e o atual (idempotencia)")
    void updatePaymentStatusNaoGravaSeIgual() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamentoComPagamento(PaymentStatus.PAID)));

        handler.updatePaymentStatus(appointmentId, PaymentStatus.PAID);

        verify(appointmentRepository, never()).updatePaymentStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("updatePaymentStatus: id de outro tenant lanca AppointmentNotFoundException")
    void updatePaymentStatusTenantErradoLancaNaoEncontrado() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.updatePaymentStatus(appointmentId, PaymentStatus.PAID))
                .isInstanceOf(AppointmentNotFoundException.class);
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
        // Reagendar é "mover", não "criar+cancelar" (DD-3 da spec técnica de
        // observabilidade) — nenhuma das duas métricas é incrementada.
        verify(schedulingMetrics, never()).appointmentCreated();
        verify(schedulingMetrics, never()).appointmentCancelled();
        verify(schedulingMetrics, never()).slotConflict();
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
        verify(schedulingMetrics).slotConflict();
        verify(schedulingMetrics, never()).appointmentCreated();
        verify(schedulingMetrics, never()).appointmentCancelled();
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
