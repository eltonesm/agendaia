package com.simboraagendar.scheduling.adapter.in.web;

import com.simboraagendar.scheduling.api.DailyScheduleDirectory;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * KPIs do painel do dono (sistema-de-design-admin, TODO-110) — mesmo
 * mecanismo de {@code billing.adapter.in.web.BillingBannerAdvice} (um
 * {@code @ControllerAdvice} global que soma atributos ao model de qualquer
 * controller), pelo mesmo motivo: {@code organization.adapter.in.web.
 * DashboardController} não pode importar {@code scheduling.application}
 * nem {@code scheduling.api} sem fechar um ciclo — {@code scheduling} já
 * depende de {@code organization.api} (`ProfessionalDirectory` etc.), e
 * `organization` depender de volta de `scheduling.api` fecha o círculo
 * (achado pelo {@code ModuleStructureTest}, Spring Modulith, ao tentar a
 * primeira versão da DD-6 da spec técnica com dependência direta).
 *
 * <p>Roda em toda requisição (mesmo custo zero de {@code BillingBannerAdvice}
 * para as demais), mas só consulta o banco quando a URI é exatamente
 * {@code /admin/dashboard} — sem isso, cada navegação por qualquer tela de
 * {@code /admin/**} pagaria a consulta à toa.
 *
 * <p>{@code DailyScheduleDirectory} chega por {@link ObjectProvider}, não
 * por injeção direta: {@code @WebMvcTest} carrega **todo**
 * {@code @ControllerAdvice} do classpath, mesmo os de outros controllers
 * (é assim que {@code BillingBannerAdvice} já funciona em qualquer fatia) —
 * mas nenhuma fatia de {@code @WebMvcTest} fora de {@code scheduling} sobe
 * {@code DailyScheduleSummaryHandler}. Sem o provider, a criação deste bean
 * falhava com {@code UnsatisfiedDependencyException} em toda classe
 * {@code @WebMvcTest} do projeto (94 testes, achado ao rodar o
 * {@code verify} completo — nenhuma fatia estava sem esse bean por acaso,
 * é o padrão inteiro de teste de camada web do projeto).
 */
@ControllerAdvice
public class DashboardKpiAdvice {

    private static final String URI_PAINEL = "/admin/dashboard";

    private final ObjectProvider<DailyScheduleDirectory> dailySchedule;

    public DashboardKpiAdvice(ObjectProvider<DailyScheduleDirectory> dailySchedule) {
        this.dailySchedule = dailySchedule;
    }

    @ModelAttribute
    public void kpisDoPainel(HttpServletRequest request, Model model) {
        if (!URI_PAINEL.equals(request.getRequestURI())) {
            return;
        }
        var directory = dailySchedule.getIfAvailable();
        if (directory == null) {
            // Só acontece em fatia de teste que não sobe scheduling.application
            // (ex.: @WebMvcTest de outro contexto) — na aplicação real o bean
            // sempre existe.
            return;
        }
        var resumo = directory.summaryFor(LocalDate.now());
        model.addAttribute("agendamentosHoje", resumo.totalToday());
        model.addAttribute("atendidosHoje", resumo.completedToday());
        model.addAttribute("receitaEstimada", resumo.estimatedRevenue());
        model.addAttribute("proximoCliente", resumo.nextClient());
    }
}
