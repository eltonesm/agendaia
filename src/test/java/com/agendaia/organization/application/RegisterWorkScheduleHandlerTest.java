package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.organization.application.command.RegisterWorkScheduleCommand;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.organization.domain.WorkSchedule;
import com.agendaia.organization.domain.exception.ProfessionalNotFoundException;
import com.agendaia.organization.domain.exception.WorkScheduleOverlapException;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Caso de uso do cadastro de jornada, sem Spring e sem banco.
 *
 * <p>{@link ProfessionalRepository} mockado — confirma a validação de
 * profissional (BR-8) sem precisar de banco (AC-3 do TASK-007).
 */
@ExtendWith(MockitoExtension.class)
class RegisterWorkScheduleHandlerTest {

    @Mock private WorkScheduleRepository workScheduleRepository;
    @Mock private ProfessionalRepository professionalRepository;

    private RegisterWorkScheduleHandler handler;

    private final UUID professionalId = UuidV7.generate();

    @BeforeEach
    void montar() {
        handler = new RegisterWorkScheduleHandler(workScheduleRepository, professionalRepository);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private RegisterWorkScheduleCommand comando(LocalTime inicio, LocalTime fim) {
        return new RegisterWorkScheduleCommand(professionalId, DayOfWeek.MONDAY, inicio, fim);
    }

    @Test
    @DisplayName("cadastra quando profissional pertence ao tenant e nao ha sobreposicao")
    void cadastraComSucesso() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(professionalRepository.existsByIdAndTenantId(professionalId, tenant.value())).thenReturn(true);
        when(workScheduleRepository.findByTenantIdAndProfessionalIdAndDayOfWeekAndActiveTrue(
                        tenant.value(), professionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of());

        var resultado = handler.register(comando(LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertThat(resultado.id()).isNotNull();
        var captor = ArgumentCaptor.forClass(WorkSchedule.class);
        verify(workScheduleRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenant);
    }

    @Test
    @DisplayName("recusa profissional que nao pertence ao tenant da sessao (BR-8)")
    void recusaProfissionalDeOutroTenant() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(professionalRepository.existsByIdAndTenantId(professionalId, tenant.value())).thenReturn(false);

        assertThatThrownBy(() -> handler.register(comando(LocalTime.of(8, 0), LocalTime.of(12, 0))))
                .isInstanceOf(ProfessionalNotFoundException.class);

        verify(workScheduleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("recusa faixa sobreposta a outra do mesmo profissional no mesmo dia (BR-3)")
    void recusaFaixaSobreposta() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(professionalRepository.existsByIdAndTenantId(professionalId, tenant.value())).thenReturn(true);
        var existente = WorkSchedule.register(tenant, professionalId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(workScheduleRepository.findByTenantIdAndProfessionalIdAndDayOfWeekAndActiveTrue(
                        tenant.value(), professionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() -> handler.register(comando(LocalTime.of(10, 0), LocalTime.of(14, 0))))
                .isInstanceOf(WorkScheduleOverlapException.class);

        verify(workScheduleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("aceita faixa encostada a outra do mesmo profissional — mecanismo do almoco")
    void aceitaFaixaEncostada() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(professionalRepository.existsByIdAndTenantId(professionalId, tenant.value())).thenReturn(true);
        var manha = WorkSchedule.register(tenant, professionalId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(workScheduleRepository.findByTenantIdAndProfessionalIdAndDayOfWeekAndActiveTrue(
                        tenant.value(), professionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(manha));

        var resultado = handler.register(comando(LocalTime.of(13, 0), LocalTime.of(18, 0)));

        assertThat(resultado.id()).isNotNull();
        verify(workScheduleRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de gravar para ninguém")
    void semTenantRecusa() {
        assertThatThrownBy(() -> handler.register(comando(LocalTime.of(8, 0), LocalTime.of(12, 0))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");

        verify(workScheduleRepository, never()).saveAndFlush(any());
    }
}
