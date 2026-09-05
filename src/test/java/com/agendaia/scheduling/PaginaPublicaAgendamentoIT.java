package com.agendaia.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.catalog.application.port.out.ServiceOfferingRepository;
import com.agendaia.catalog.application.port.out.ServiceRepository;
import com.agendaia.catalog.domain.Service;
import com.agendaia.catalog.domain.ServiceOffering;
import com.agendaia.customer.application.port.out.CustomerRepository;
import com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository;
import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessOperatingHours;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.WorkSchedule;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.shared.Money;
import com.agendaia.shared.UuidV7;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * E2E-1 a E2E-7 da spec funcional de pagina-publica-agendamento, ponta a
 * ponta contra Postgres real via Testcontainers — obrigatório para a
 * exclusion constraint do ADR 0005, que H2 não implementa.
 *
 * <p>Cada teste cria seu próprio estabelecimento com slug único (UUID), em
 * vez de limpar tabelas entre métodos — evita depender de um repositório
 * de {@code Appointment}/{@code Customer} exposto só para teste (a porta
 * pública {@code AppointmentRepository} não tem {@code deleteAll}, de
 * propósito) e isola o teste de concorrência (E2E-2) de qualquer outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PaginaPublicaAgendamentoIT {

    private static final DayOfWeek DIA = DayOfWeek.MONDAY;
    private static final LocalDate SEGUNDA = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private ServiceOfferingRepository serviceOfferingRepository;
    @Autowired private BusinessOperatingHoursRepository businessOperatingHoursRepository;
    @Autowired private WorkScheduleRepository workScheduleRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    /** Um estabelecimento completo (expediente + jornada) pronto para agendar, isolado por slug único. */
    private record Cenario(Business barbearia, Professional profissional, ServiceOffering oferta) {}

    private Cenario semearCenario() {
        var sufixo = UuidV7.generate().toString();
        var barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia E2E " + sufixo, "barbearia-e2e-" + sufixo));
        var profissional =
                professionalRepository.saveAndFlush(Professional.register(barbearia.tenantId(), "Profissional " + sufixo));
        var servico = serviceRepository.saveAndFlush(Service.register(barbearia.tenantId(), "Corte " + sufixo, null));
        var oferta = serviceOfferingRepository.saveAndFlush(ServiceOffering.register(
                barbearia.tenantId(), servico.id(), profissional.id(), 30, Money.reais(new BigDecimal("30.00")), 0));
        businessOperatingHoursRepository.saveAndFlush(
                BusinessOperatingHours.register(barbearia.tenantId(), DIA, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        workScheduleRepository.saveAndFlush(
                WorkSchedule.register(barbearia.tenantId(), profissional.id(), DIA, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        return new Cenario(barbearia, profissional, oferta);
    }

    private ResultActions postConfirmar(
            Cenario cenario, LocalTime horario, String name, String phone, String website, RequestPostProcessor ip)
            throws Exception {
        return mockMvc.perform(post("/b/{slug}/ofertas/{offeringId}", cenario.barbearia().slug(), cenario.oferta().id())
                .with(csrf())
                .with(ip)
                .param("startsAt", SEGUNDA.atTime(horario).toString())
                .param("data", SEGUNDA.toString())
                .param("name", name)
                .param("phone", phone)
                .param("website", website));
    }

    private static RequestPostProcessor ip(String endereco) {
        return request -> {
            request.setRemoteAddr(endereco);
            return request;
        };
    }

    @Test
    @DisplayName("E2E-1: caminho feliz — agendamento criado, Customer novo criado")
    void e2e1CaminhoFeliz() throws Exception {
        var cenario = semearCenario();

        postConfirmar(cenario, LocalTime.of(8, 0), "Joao da Silva", "11988887001", "", ip("10.1.0.1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/b/**/agendamentos/**"));

        var ocupados = appointmentRepository.findOccupiedRanges(
                cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA);
        assertThat(ocupados).hasSize(1);
        assertThat(ocupados.getFirst().start()).isEqualTo(LocalTime.of(8, 0));

        var cliente = customerRepository.findByTenantIdAndPhone(cenario.barbearia().tenantId().value(), "11988887001");
        assertThat(cliente).isPresent();
        assertThat(cliente.get().name()).isEqualTo("Joao da Silva");
    }

    @Test
    @DisplayName("E2E-2: duas reservas concorrentes para o mesmo horário — só uma vence")
    void e2e2ConcorrenciaSoUmaVence() throws Exception {
        var cenario = semearCenario();
        var horario = LocalTime.of(9, 0);

        var largada = new CountDownLatch(1);
        var pronto = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 2; i++) {
                var indice = i;
                pool.submit(() -> {
                    try {
                        pronto.countDown();
                        largada.await();
                        postConfirmar(
                                cenario,
                                horario,
                                "Cliente Concorrente " + indice,
                                "1199888800" + indice,
                                "",
                                ip("10.1.0.1" + indice));
                    } catch (Exception ignorado) {
                        // O resultado (sucesso ou erro tratado) é medido no banco depois, não pela exceção da thread.
                    }
                });
            }
            pronto.await(5, TimeUnit.SECONDS);
            largada.countDown();
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }

        var ocupados = appointmentRepository.findOccupiedRanges(
                cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA);
        assertThat(ocupados).hasSize(1);
        assertThat(ocupados.getFirst().start()).isEqualTo(horario);
    }

    @Test
    @DisplayName("E2E-3: id de oferta de outro tenant é recusado, nada é gravado")
    void e2e3OutroTenantERecusado() throws Exception {
        var cenarioA = semearCenario();
        var cenarioB = semearCenario();

        // Confirma no slug do tenant A, mas com o offeringId do tenant B —
        // mesmo tratamento do GET (404): recarregar a tela de horários
        // dessa mesma oferta inválida só repetiria o erro (DD, achado no
        // teste manual/E2E-3).
        mockMvc.perform(post("/b/{slug}/ofertas/{offeringId}", cenarioA.barbearia().slug(), cenarioB.oferta().id())
                        .with(csrf())
                        .with(ip("10.1.0.20"))
                        .param("startsAt", SEGUNDA.atTime(10, 0).toString())
                        .param("data", SEGUNDA.toString())
                        .param("name", "Tentativa Forjada")
                        .param("phone", "11988887003")
                        .param("website", ""))
                .andExpect(status().isNotFound());

        assertThat(appointmentRepository.findOccupiedRanges(
                        cenarioB.barbearia().tenantId(), cenarioB.profissional().id(), SEGUNDA))
                .isEmpty();
    }

    @Test
    @DisplayName("E2E-4: honeypot preenchido não grava Appointment")
    void e2e4HoneypotNaoGrava() throws Exception {
        var cenario = semearCenario();

        postConfirmar(cenario, LocalTime.of(11, 0), "Bot", "11900000004", "http://spam.example", ip("10.1.0.30"))
                .andExpect(status().is3xxRedirection());

        assertThat(appointmentRepository.findOccupiedRanges(
                        cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA))
                .isEmpty();
    }

    @Test
    @DisplayName("E2E-5: 6a tentativa do mesmo IP em 10 min é recusada por rate limit")
    void e2e5RateLimit() throws Exception {
        var cenario = semearCenario();
        var meuIp = ip("10.1.0.40");

        for (int i = 0; i < 5; i++) {
            postConfirmar(cenario, LocalTime.of(12, 0).plusMinutes(30L * i), "Cliente " + i, "119999900" + i, "", meuIp)
                    .andExpect(status().is3xxRedirection());
        }

        postConfirmar(cenario, LocalTime.of(14, 30), "Sexto Cliente", "11999990099", "", meuIp)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Muitas tentativas")));

        assertThat(appointmentRepository.findOccupiedRanges(
                        cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA))
                .hasSize(5);
    }

    @Test
    @DisplayName("E2E-6: telefone com 3 agendamentos futuros ativos recusa o 4o")
    void e2e6TetoPorTelefone() throws Exception {
        var cenario = semearCenario();
        var telefone = "11988887006";

        for (int i = 0; i < 3; i++) {
            postConfirmar(cenario, LocalTime.of(14, 0).plusMinutes(30L * i), "Cliente Teto", telefone, "", ip("10.1.0.5" + i))
                    .andExpect(status().is3xxRedirection());
        }

        postConfirmar(cenario, LocalTime.of(16, 0), "Cliente Teto", telefone, "", ip("10.1.0.60"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("máximo de agendamentos")));

        assertThat(appointmentRepository.findOccupiedRanges(
                        cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA))
                .hasSize(3);
    }

    @Test
    @DisplayName("E2E-7: telefone repetido reaproveita o mesmo Customer, nunca duplica")
    void e2e7CustomerReaproveitado() throws Exception {
        var cenario = semearCenario();
        var telefone = "11988887007";

        postConfirmar(cenario, LocalTime.of(16, 0), "Nome Antigo", telefone, "", ip("10.1.0.70"))
                .andExpect(status().is3xxRedirection());
        postConfirmar(cenario, LocalTime.of(16, 30), "Nome Novo", telefone, "", ip("10.1.0.71"))
                .andExpect(status().is3xxRedirection());

        var cliente = customerRepository.findByTenantIdAndPhone(cenario.barbearia().tenantId().value(), telefone);
        assertThat(cliente).isPresent();
        assertThat(cliente.get().name()).isEqualTo("Nome Novo");

        assertThat(appointmentRepository.findOccupiedRanges(
                        cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA))
                .hasSize(2);
    }
}
