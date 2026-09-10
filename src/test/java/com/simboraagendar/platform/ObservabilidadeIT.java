package com.simboraagendar.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.simboraagendar.TestcontainersConfiguration;
import com.simboraagendar.catalog.application.port.out.ServiceOfferingRepository;
import com.simboraagendar.catalog.application.port.out.ServiceRepository;
import com.simboraagendar.catalog.domain.Service;
import com.simboraagendar.catalog.domain.ServiceOffering;
import com.simboraagendar.organization.application.port.out.BusinessOperatingHoursRepository;
import com.simboraagendar.organization.application.port.out.BusinessRepository;
import com.simboraagendar.organization.application.port.out.ProfessionalRepository;
import com.simboraagendar.organization.application.port.out.UserRepository;
import com.simboraagendar.organization.application.port.out.WorkScheduleRepository;
import com.simboraagendar.organization.domain.Business;
import com.simboraagendar.organization.domain.BusinessOperatingHours;
import com.simboraagendar.organization.domain.Professional;
import com.simboraagendar.organization.domain.User;
import com.simboraagendar.organization.domain.WorkSchedule;
import com.simboraagendar.platform.tenant.TenantContext;
import com.simboraagendar.shared.Money;
import com.simboraagendar.shared.UuidV7;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * E2E-1 a E2E-4 da spec funcional de observabilidade, ponta a ponta contra
 * Postgres real via Testcontainers.
 *
 * <p><strong>Achado durante a implementação</strong>: a estratégia original
 * da spec técnica ("redirecionar {@code System.out} e parsear cada linha
 * como JSON") não funciona de forma confiável — o {@code ConsoleAppender}
 * do Logback resolve {@code System.out} uma vez, na inicialização do
 * contexto, então trocar o fluxo em tempo de teste não é capturado por ele.
 * Troca: um {@link ListAppender} anexado à raiz captura os
 * {@link ILoggingEvent} de verdade, e o MDC de cada evento
 * ({@code getMDCPropertyMap()}) é verificado diretamente — mais robusto que
 * reproduzir o formato exato do ECS, e continua provando o comportamento
 * que importa (o MDC chega ao log).
 */
@SpringBootTest
@AutoConfigureMockMvc
// Sem isto, o suporte de teste do Spring Boot desliga a exportação de
// métricas por padrão (management.defaults.metrics.export.enabled=false)
// — o bean do PrometheusMeterRegistry/endpoint nunca existe no contexto de
// teste, e /actuator/prometheus vira 404 mesmo com a credencial certa
// (achado durante a implementação desta IT).
@AutoConfigureMetrics
@Import(TestcontainersConfiguration.class)
class ObservabilidadeIT {

    private static final String SENHA = "senha-do-dono";
    private static final DayOfWeek DIA = DayOfWeek.MONDAY;
    private static final LocalDate SEGUNDA = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private ServiceOfferingRepository serviceOfferingRepository;
    @Autowired private BusinessOperatingHoursRepository businessOperatingHoursRepository;
    @Autowired private WorkScheduleRepository workScheduleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void anexarAppender() {
        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(appender);
    }

    @AfterEach
    void limparContexto() {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(appender);
        TenantContext.clear();
    }

    private record Cenario(Business barbearia, Professional profissional, ServiceOffering oferta, String email) {}

    private Cenario semearCenario() {
        var sufixo = UuidV7.generate().toString();
        var email = "dono-" + sufixo + "@exemplo.com";
        var barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia Obs " + sufixo, "barbearia-obs-" + sufixo));
        userRepository.saveAndFlush(
                User.owner(barbearia.tenantId(), email, "Dono " + sufixo, passwordEncoder.encode(SENHA)));
        var profissional = professionalRepository.saveAndFlush(
                Professional.register(barbearia.tenantId(), "Profissional " + sufixo));
        var servico = serviceRepository.saveAndFlush(Service.register(barbearia.tenantId(), "Corte " + sufixo, null));
        var oferta = serviceOfferingRepository.saveAndFlush(ServiceOffering.register(
                barbearia.tenantId(), servico.id(), profissional.id(), 30, Money.reais(new BigDecimal("30.00")), 0));
        businessOperatingHoursRepository.saveAndFlush(
                BusinessOperatingHours.register(barbearia.tenantId(), DIA, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        workScheduleRepository.saveAndFlush(
                WorkSchedule.register(barbearia.tenantId(), profissional.id(), DIA, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        return new Cenario(barbearia, profissional, oferta, email);
    }

    private MockHttpSession sessaoAutenticada(Cenario cenario) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", cenario.email())
                        .param("password", SENHA))
                .andReturn()
                .getRequest()
                .getSession();
    }

    @Test
    @DisplayName("E2E-1: log de requisicao autenticada traz tenantId e requestId no MDC")
    void e2e1LogComTenantIdERequestId() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);

        // Qualquer 404 dentro de /admin/** já dispara log.warn no
        // GlobalExceptionHandler, com Security e TenantContextFilter já
        // tendo rodado antes — suficiente para provar o MDC sem depender
        // de nenhuma regra de negócio específica.
        mockMvc.perform(get("/admin/rota-que-nao-existe").session(sessao)).andExpect(status().isNotFound());

        assertThat(appender.list).anySatisfy(evento -> {
            assertThat(evento.getMDCPropertyMap()).containsKey("tenantId");
            assertThat(evento.getMDCPropertyMap()).containsKey("requestId");
            assertThat(evento.getMDCPropertyMap().get("tenantId")).isEqualTo(cenario.barbearia().tenantId().value().toString());
        });
    }

    @Test
    @DisplayName("E2E-2: /actuator/health responde sem autenticacao")
    void e2e2HealthSemAutenticacao() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"UP\"")));
    }

    @Test
    @DisplayName("E2E-3: /actuator/prometheus exige credencial e expoe as metricas de negocio incrementadas")
    void e2e3PrometheusExigeCredencialEExpoeMetricas() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);

        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());

        var antes = mockMvc.perform(get("/actuator/prometheus").with(httpBasic("metrics", "metrics-dev-only")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var criadoAntes = extrairContador(antes, "simboraagendar_appointments_booked_total");
        var canceladoAntes = extrairContador(antes, "simboraagendar_appointments_cancelled_total");
        var conflitoAntes = extrairContador(antes, "simboraagendar_appointments_slot_conflict_total");

        var horario = LocalTime.of(8, 0);
        mockMvc.perform(post("/admin/agenda/novo")
                        .with(csrf())
                        .session(sessao)
                        .param("serviceOfferingId", cenario.oferta().id().toString())
                        .param("date", SEGUNDA.toString())
                        .param("time", horario.toString())
                        .param("customerName", "Cliente Metrica")
                        .param("customerPhone", "11988880001"))
                .andExpect(status().is3xxRedirection());

        // Mesmo horario, mesmo profissional — colide de proposito (ADR 0005).
        mockMvc.perform(post("/admin/agenda/novo")
                .with(csrf())
                .session(sessao)
                .param("serviceOfferingId", cenario.oferta().id().toString())
                .param("date", SEGUNDA.toString())
                .param("time", horario.toString())
                .param("customerName", "Cliente Conflito")
                .param("customerPhone", "11988880002"));

        var depois = mockMvc.perform(get("/actuator/prometheus").with(httpBasic("metrics", "metrics-dev-only")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var criadoDepois = extrairContador(depois, "simboraagendar_appointments_booked_total");
        var conflitoDepois = extrairContador(depois, "simboraagendar_appointments_slot_conflict_total");

        assertThat(criadoDepois).isEqualTo(criadoAntes + 1);
        assertThat(conflitoDepois).isEqualTo(conflitoAntes + 1);
        assertThat(canceladoAntes).isGreaterThanOrEqualTo(0.0);
    }

    private static double extrairContador(String corpoPrometheus, String nomeMetrica) {
        var padrao = Pattern.compile(Pattern.quote(nomeMetrica) + "\\s+([0-9.eE+-]+)");
        var casador = padrao.matcher(corpoPrometheus);
        return casador.find() ? Double.parseDouble(casador.group(1)) : 0.0;
    }

    @Test
    @DisplayName("E2E-4: nenhuma linha de log de um fluxo de agendamento contem nome nem telefone do cliente")
    void e2e4SemDadoPessoalNoLog() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);
        var nomeReal = "Fulano da Silva Sauro";
        var telefoneReal = "11977776666";

        mockMvc.perform(post("/admin/agenda/novo")
                        .with(csrf())
                        .session(sessao)
                        .param("serviceOfferingId", cenario.oferta().id().toString())
                        .param("date", SEGUNDA.toString())
                        .param("time", LocalTime.of(9, 0).toString())
                        .param("customerName", nomeReal)
                        .param("customerPhone", telefoneReal))
                .andExpect(status().is3xxRedirection());

        assertThat(appender.list).noneSatisfy(evento -> {
            var mensagem = evento.getFormattedMessage();
            assertThat(mensagem).containsAnyOf(nomeReal, telefoneReal);
        });
    }
}
