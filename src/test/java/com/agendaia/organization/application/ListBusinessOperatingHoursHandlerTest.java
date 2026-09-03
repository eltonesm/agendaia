package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository;
import com.agendaia.organization.domain.BusinessOperatingHours;
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

/** Caso de uso da listagem de horário de funcionamento, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class ListBusinessOperatingHoursHandlerTest {

    @Mock private BusinessOperatingHoursRepository businessOperatingHoursRepository;

    private ListBusinessOperatingHoursHandler handler;

    @BeforeEach
    void montar() {
        handler = new ListBusinessOperatingHoursHandler(businessOperatingHoursRepository);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("lista as faixas do tenant do contexto")
    void listaDoTenantDoContexto() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        var faixa = BusinessOperatingHours.register(tenant, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0));
        when(businessOperatingHoursRepository.findByTenantIdAndActiveTrueOrderByDayOfWeekAscOpensAtAsc(tenant.value()))
                .thenReturn(List.of(faixa));

        var lista = handler.list();

        assertThat(lista).hasSize(1);
        assertThat(lista.getFirst().id()).isEqualTo(faixa.id());
        assertThat(lista.getFirst().dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de devolver a lista de alguém")
    void semTenantRecusa() {
        assertThatThrownBy(handler::list)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");
    }

    @Test
    @DisplayName("estabelecimento sem faixa cadastrada devolve lista vazia, nao erro")
    void semFaixaDevolveListaVazia() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(businessOperatingHoursRepository.findByTenantIdAndActiveTrueOrderByDayOfWeekAscOpensAtAsc(tenant.value()))
                .thenReturn(List.of());

        assertThat(handler.list()).isEmpty();
    }
}
