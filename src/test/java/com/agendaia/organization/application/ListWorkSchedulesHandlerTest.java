package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.WorkSchedule;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso da listagem de jornada, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class ListWorkSchedulesHandlerTest {

    @Mock private WorkScheduleRepository workScheduleRepository;
    @Mock private ProfessionalRepository professionalRepository;

    private ListWorkSchedulesHandler handler;

    @BeforeEach
    void montar() {
        handler = new ListWorkSchedulesHandler(workScheduleRepository, professionalRepository);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("resolve o nome do profissional na projecao, nao id cru")
    void resolveNomeNaProjecao() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        var profissional = Professional.register(tenant, "João da Silva");
        var faixa = WorkSchedule.register(
                tenant, profissional.id(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

        when(workScheduleRepository.findByTenantIdAndActiveTrueOrderByProfessionalIdAscDayOfWeekAscStartsAtAsc(
                        tenant.value()))
                .thenReturn(List.of(faixa));
        when(professionalRepository.findAllById(List.of(profissional.id()))).thenReturn(List.of(profissional));

        var lista = handler.list();

        assertThat(lista).hasSize(1);
        var linha = lista.getFirst();
        assertThat(linha.id()).isEqualTo(faixa.id());
        assertThat(linha.professionalName()).isEqualTo("João da Silva");
        assertThat(linha.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de devolver a lista de alguém")
    void semTenantRecusa() {
        assertThatThrownBy(handler::list)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");
    }

    @Test
    @DisplayName("estabelecimento sem faixa devolve lista vazia, nao erro")
    void semFaixaDevolveListaVazia() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(workScheduleRepository.findByTenantIdAndActiveTrueOrderByProfessionalIdAscDayOfWeekAscStartsAtAsc(
                        tenant.value()))
                .thenReturn(List.of());
        when(professionalRepository.findAllById(List.of())).thenReturn(List.of());

        assertThat(handler.list()).isEmpty();
    }
}
