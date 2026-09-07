package com.agendaia.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.customer.api.CustomerDirectory;
import com.agendaia.customer.api.CustomerRef;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.organization.api.ProfessionalRef;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.exception.AppointmentNotFoundException;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Orquestração isolada, sem Spring e sem banco — as três portas de ManageAppointmentHandler. */
@ExtendWith(MockitoExtension.class)
class ManageAppointmentHandlerTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ProfessionalDirectory professionalDirectory;
    @Mock private CustomerDirectory customerDirectory;

    private ManageAppointmentHandler handler;

    private final TenantId tenant = TenantId.of(UuidV7.generate());
    private final UUID professionalId = UuidV7.generate();
    private final UUID customerId = UuidV7.generate();
    private final UUID appointmentId = UuidV7.generate();
    private final Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);

    @BeforeEach
    void montar() {
        handler = new ManageAppointmentHandler(appointmentRepository, professionalDirectory, customerDirectory);
        TenantContext.set(tenant);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private Appointment agendamento(AppointmentStatus status) {
        return Appointment.reconstitute(
                appointmentId,
                tenant,
                professionalId,
                UuidV7.generate(),
                customerId,
                status,
                startsAt,
                startsAt.plus(30, ChronoUnit.MINUTES),
                "Corte de Cabelo",
                30,
                new Money(3000));
    }

    private void mockarNomes() {
        when(professionalDirectory.find(professionalId))
                .thenReturn(Optional.of(new ProfessionalRef(professionalId, "Maria")));
        when(customerDirectory.find(customerId))
                .thenReturn(Optional.of(new CustomerRef(customerId, "João", "+5511999990000")));
    }

    @Test
    @DisplayName("handle: id de outro tenant ou inexistente lança AppointmentNotFoundException (AC-1)")
    void handleTenantErradoLancaNaoEncontrado() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(appointmentId)).isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    @DisplayName("handle: monta AppointmentDetails com nomes resolvidos e canConfirm/canCancel corretos")
    void handleMontaDetalhesComAcoesDisponiveis() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.SCHEDULED)));
        mockarNomes();

        var detalhes = handler.handle(appointmentId);

        assertThat(detalhes.professionalName()).isEqualTo("Maria");
        assertThat(detalhes.customerName()).isEqualTo("João");
        assertThat(detalhes.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(detalhes.canConfirm()).isTrue();
        assertThat(detalhes.canCancel()).isTrue();
    }

    @Test
    @DisplayName("handle: agendamento CANCELLED não permite confirmar nem cancelar")
    void handleCanceladoSemAcoesDisponiveis() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.CANCELLED)));
        mockarNomes();

        var detalhes = handler.handle(appointmentId);

        assertThat(detalhes.canConfirm()).isFalse();
        assertThat(detalhes.canCancel()).isFalse();
    }

    @Test
    @DisplayName("confirm: grava updateStatus só quando há transição real (AC-2)")
    void confirmGravaSoQuandoMuda() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.SCHEDULED)));

        handler.confirm(appointmentId);

        verify(appointmentRepository).updateStatus(eq(tenant), eq(appointmentId), eq(AppointmentStatus.CONFIRMED), any());
    }

    @Test
    @DisplayName("confirm: agendamento já CANCELLED não grava nada")
    void confirmSemEfeitoQuandoCancelado() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.CANCELLED)));

        handler.confirm(appointmentId);

        verify(appointmentRepository, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("cancel: grava updateStatus só quando há transição real (AC-3)")
    void cancelGravaSoQuandoMuda() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.CONFIRMED)));

        handler.cancel(appointmentId);

        verify(appointmentRepository).updateStatus(eq(tenant), eq(appointmentId), eq(AppointmentStatus.CANCELLED), any());
    }

    @Test
    @DisplayName("cancel: agendamento já CANCELLED não grava nada (idempotência)")
    void cancelSemEfeitoQuandoJaCancelado() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId))
                .thenReturn(Optional.of(agendamento(AppointmentStatus.CANCELLED)));

        handler.cancel(appointmentId);

        verify(appointmentRepository, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("confirm: id de outro tenant lança AppointmentNotFoundException, nunca grava")
    void confirmTenantErradoLancaNaoEncontrado() {
        when(appointmentRepository.findByTenantIdAndId(tenant, appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.confirm(appointmentId)).isInstanceOf(AppointmentNotFoundException.class);

        verify(appointmentRepository, never()).updateStatus(any(), any(), any(), any());
    }
}
