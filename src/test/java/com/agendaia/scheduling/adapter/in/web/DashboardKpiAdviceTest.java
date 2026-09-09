package com.agendaia.scheduling.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.scheduling.api.DailyScheduleDirectory;
import com.agendaia.scheduling.api.DailyScheduleSummary;
import com.agendaia.scheduling.api.NextClientRef;
import com.agendaia.shared.Money;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

/**
 * Sem Spring — MockHttpServletRequest e um Model real bastam. Prova as duas
 * pontas do achado que corrigiu o ciclo de módulo da DD-6 (spec técnica):
 * o advice só consulta o banco na URI do painel, nunca nas outras telas de
 * /admin/**. {@code DailyScheduleDirectory} chega por {@link ObjectProvider}
 * (não injeção direta) — ver o porquê no javadoc de {@link DashboardKpiAdvice}.
 */
@ExtendWith(MockitoExtension.class)
class DashboardKpiAdviceTest {

    @Mock private DailyScheduleDirectory dailySchedule;

    @Mock private ObjectProvider<DailyScheduleDirectory> dailyScheduleProvider;

    @Test
    @DisplayName("na URI do painel, soma os 4 KPIs ao model")
    void naUriDoPainelSomaOsKpis() {
        var advice = new DashboardKpiAdvice(dailyScheduleProvider);
        var request = new MockHttpServletRequest("GET", "/admin/dashboard");
        var proximo = new NextClientRef("João", Instant.now());
        when(dailyScheduleProvider.getIfAvailable()).thenReturn(dailySchedule);
        when(dailySchedule.summaryFor(any())).thenReturn(new DailyScheduleSummary(3, 1, new Money(12000), proximo));
        var model = new ExtendedModelMap();

        advice.kpisDoPainel(request, model);

        assertThat(model.getAttribute("agendamentosHoje")).isEqualTo(3L);
        assertThat(model.getAttribute("atendidosHoje")).isEqualTo(1L);
        assertThat(model.getAttribute("receitaEstimada")).isEqualTo(new Money(12000));
        assertThat(model.getAttribute("proximoCliente")).isEqualTo(proximo);
    }

    @Test
    @DisplayName("fora da URI do painel, nao consulta o provider nem toca o model")
    void foraDaUriNaoConsultaNemTocaModel() {
        var advice = new DashboardKpiAdvice(dailyScheduleProvider);
        var request = new MockHttpServletRequest("GET", "/admin/agenda");
        var model = new ExtendedModelMap();

        advice.kpisDoPainel(request, model);

        verify(dailyScheduleProvider, never()).getIfAvailable();
        assertThat(model.asMap()).isEmpty();
    }

    @Test
    @DisplayName("bean ausente (fatia de teste sem scheduling.application) nao quebra nem toca o model")
    void beanAusenteNaoQuebra() {
        var advice = new DashboardKpiAdvice(dailyScheduleProvider);
        var request = new MockHttpServletRequest("GET", "/admin/dashboard");
        when(dailyScheduleProvider.getIfAvailable()).thenReturn(null);
        var model = new ExtendedModelMap();

        advice.kpisDoPainel(request, model);

        assertThat(model.asMap()).isEmpty();
    }
}
