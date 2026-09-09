package com.agendaia.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.catalog.application.port.out.ServiceOfferingRepository;
import com.agendaia.catalog.application.port.out.ServiceRepository;
import com.agendaia.catalog.domain.Service;
import com.agendaia.catalog.domain.ServiceOffering;
import com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository;
import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.UserRepository;
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessOperatingHours;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.User;
import com.agendaia.organization.domain.WorkSchedule;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.PaymentStatus;
import com.agendaia.shared.Money;
import com.agendaia.shared.UuidV7;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * E2E-1 a E2E-7 da spec funcional de agenda-profissional, ponta a ponta
 * contra Postgres real via Testcontainers — obrigatório para a exclusion
 * constraint do ADR 0005 (E2E-2, concorrência painel x link público).
 *
 * <p>Cada teste cria seu próprio estabelecimento com slug único, mesma
 * técnica de {@code ConfirmacaoECancelamentoIT} (TODO-007) — sem
 * {@code @BeforeEach} de limpeza, sem dependência de ordem entre testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AgendaProfissionalIT {

    private static final String SENHA = "senha-do-dono";
    private static final DayOfWeek DIA = DayOfWeek.MONDAY;
    private static final LocalDate SEGUNDA = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private static final LocalDate ONTEM = LocalDate.now().minusDays(1);

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private ServiceOfferingRepository serviceOfferingRepository;
    @Autowired private BusinessOperatingHoursRepository businessOperatingHoursRepository;
    @Autowired private WorkScheduleRepository workScheduleRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private record Cenario(Business barbearia, Professional profissional, ServiceOffering oferta, String email) {}

    private Cenario semearCenario() {
        var sufixo = UuidV7.generate().toString();
        var email = "dono-" + sufixo + "@exemplo.com";
        var barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia E2E " + sufixo, "barbearia-e2e-" + sufixo));
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

    private ResultActions criarManualmente(
            Cenario cenario, MockHttpSession sessao, LocalDate data, LocalTime horario, String nome, String telefone)
            throws Exception {
        return mockMvc.perform(post("/admin/agenda/novo")
                .with(csrf())
                .session(sessao)
                .param("serviceOfferingId", cenario.oferta().id().toString())
                .param("date", data.toString())
                .param("time", horario.toString())
                .param("customerName", nome)
                .param("customerPhone", telefone));
    }

    /**
     * A criação manual redireciona só para {@code /admin/agenda?date=...}, sem o id no path
     * (simplificado: nenhuma tela própria depende dele) — o teste resolve o id consultando o
     * agregado de verdade pelo horário exato, único por causa da exclusion constraint (ADR 0005).
     */
    private UUID idPorHorario(Cenario cenario, LocalDate data, LocalTime horario) {
        var instanteEsperado =
                LocalDateTime.of(data, horario).atZone(java.time.ZoneId.systemDefault()).toInstant();
        return appointmentRepository
                .findByTenantIdAndProfessionalIdAndDate(cenario.barbearia().tenantId(), cenario.profissional().id(), data)
                .stream()
                .filter(a -> a.startsAt().equals(instanteEsperado))
                .findFirst()
                .orElseThrow()
                .id();
    }

    @Test
    @DisplayName("E2E-1: dono cria agendamento manualmente e ele aparece na agenda do dia")
    void e2e1CriarManualmente() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);

        criarManualmente(cenario, sessao, SEGUNDA, LocalTime.of(8, 0), "Cliente Manual", "11988880001")
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/agenda")
                        .session(sessao)
                        .param("professionalId", cenario.profissional().id().toString())
                        .param("date", SEGUNDA.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cliente Manual")));
    }

    @Test
    @DisplayName("E2E-2: criação manual pelo painel e reserva pública concorrem pelo mesmo horário — só uma vence (ADR 0005)")
    void e2e2ConcorrenciaPainelXPublico() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);
        var horario = LocalTime.of(14, 0);

        var pronto = new CountDownLatch(2);
        var partida = new CountDownLatch(1);
        var fim = new CountDownLatch(2);
        var statusPorCanal = new ConcurrentHashMap<String, Integer>();

        var threadPainel = new Thread(() -> {
            try {
                pronto.countDown();
                partida.await();
                var status = criarManualmente(cenario, sessao, SEGUNDA, horario, "Cliente Painel", "11988880090")
                        .andReturn()
                        .getResponse()
                        .getStatus();
                statusPorCanal.put("painel", status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                fim.countDown();
            }
        });

        var threadPublico = new Thread(() -> {
            try {
                pronto.countDown();
                partida.await();
                var status = mockMvc.perform(post(
                                "/b/{slug}/ofertas/{offeringId}", cenario.barbearia().slug(), cenario.oferta().id())
                                .with(csrf())
                                .param("startsAt", LocalDateTime.of(SEGUNDA, horario).toString())
                                .param("data", SEGUNDA.toString())
                                .param("name", "Cliente Publico")
                                .param("phone", "11988880091")
                                .param("website", ""))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                statusPorCanal.put("publico", status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                fim.countDown();
            }
        });

        threadPainel.start();
        threadPublico.start();
        pronto.await(5, TimeUnit.SECONDS);
        partida.countDown();
        assertThat(fim.await(15, TimeUnit.SECONDS)).isTrue();

        var vencedores =
                statusPorCanal.values().stream().filter(status -> status == 302).count();
        assertThat(vencedores).isEqualTo(1);

        var ativosNoHorario = appointmentRepository
                .findByTenantIdAndProfessionalIdAndDate(
                        cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA)
                .stream()
                .filter(a -> a.status() != AppointmentStatus.CANCELLED)
                .filter(a -> a.startsAt()
                        .equals(LocalDateTime.of(SEGUNDA, horario)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()))
                .toList();
        assertThat(ativosNoHorario).hasSize(1);
    }

    @Test
    @DisplayName("E2E-3: dono cancela agendamento com horário já passado — sem restrição de tempo (BR-2)")
    void e2e3CancelarAgendamentoPassado() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);
        var horario = LocalTime.of(9, 0);
        criarManualmente(cenario, sessao, ONTEM, horario, "Cliente Passado", "11988880003")
                .andExpect(status().is3xxRedirection());
        var id = idPorHorario(cenario, ONTEM, horario);

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/cancelar", id).with(csrf()).session(sessao))
                .andExpect(status().is3xxRedirection());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), id);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("E2E-4: reagendar cria um novo agendamento e cancela o antigo, sem apagar histórico (ADR 0011)")
    void e2e4ReagendarPreservaHistorico() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);
        var horarioAntigo = LocalTime.of(10, 0);
        criarManualmente(cenario, sessao, SEGUNDA, horarioAntigo, "Cliente Reagenda", "11988880004")
                .andExpect(status().is3xxRedirection());
        var idAntigo = idPorHorario(cenario, SEGUNDA, horarioAntigo);
        var novoHorario = LocalTime.of(11, 0);

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/reagendar", idAntigo)
                        .with(csrf())
                        .session(sessao)
                        .param("serviceOfferingId", cenario.oferta().id().toString())
                        .param("date", SEGUNDA.toString())
                        .param("time", novoHorario.toString()))
                .andExpect(status().is3xxRedirection());

        var antigo = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), idAntigo);
        assertThat(antigo).isPresent();
        assertThat(antigo.get().status()).isEqualTo(AppointmentStatus.CANCELLED);

        var doDia = appointmentRepository.findByTenantIdAndProfessionalIdAndDate(
                cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA);
        assertThat(doDia).hasSize(2);
        assertThat(doDia.stream().filter(a -> a.status() == AppointmentStatus.SCHEDULED).toList()).hasSize(1);
    }

    @Test
    @DisplayName("E2E-5: id de outro tenant nas rotas de ação devolve 404, nunca altera o agendamento")
    void e2e5IsolamentoEntreTenants() throws Exception {
        var cenarioA = semearCenario();
        var cenarioB = semearCenario();
        var sessaoA = sessaoAutenticada(cenarioA);
        var horario = LocalTime.of(12, 0);
        criarManualmente(cenarioB, sessaoAutenticada(cenarioB), SEGUNDA, horario, "Cliente B", "11988880005")
                .andExpect(status().is3xxRedirection());
        var idDoB = idPorHorario(cenarioB, SEGUNDA, horario);

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/cancelar", idDoB).with(csrf()).session(sessaoA))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/confirmar", idDoB).with(csrf()).session(sessaoA))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/admin/agenda/agendamentos/{id}/reagendar", idDoB).session(sessaoA))
                .andExpect(status().isNotFound());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenarioB.barbearia().tenantId(), idDoB);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().status()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("E2E-6: teto de 3 agendamentos futuros por telefone (BR-9 da TODO-006) não se aplica à criação manual (BR-4)")
    void e2e6TetoNaoSeAplicaAoDono() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);
        var telefone = "11988880006";

        criarManualmente(cenario, sessao, SEGUNDA, LocalTime.of(8, 0), "Cliente Teto", telefone)
                .andExpect(status().is3xxRedirection());
        criarManualmente(cenario, sessao, SEGUNDA, LocalTime.of(8, 30), "Cliente Teto", telefone)
                .andExpect(status().is3xxRedirection());
        criarManualmente(cenario, sessao, SEGUNDA, LocalTime.of(9, 0), "Cliente Teto", telefone)
                .andExpect(status().is3xxRedirection());
        // Um 4o agendamento futuro ativo para o mesmo telefone — bloquearia no fluxo público (BR-9), não aqui.
        criarManualmente(cenario, sessao, SEGUNDA, LocalTime.of(9, 30), "Cliente Teto", telefone)
                .andExpect(status().is3xxRedirection());

        var doDia = appointmentRepository.findByTenantIdAndProfessionalIdAndDate(
                cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA);
        assertThat(doDia.stream().filter(a -> a.status() == AppointmentStatus.SCHEDULED).toList())
                .hasSize(4);
    }

    @Test
    @DisplayName("E2E-7: dono confirma presença pelo painel — SCHEDULED vira CONFIRMED")
    void e2e7ConfirmarPresencaPeloPainel() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);
        var horario = LocalTime.of(13, 0);
        criarManualmente(cenario, sessao, SEGUNDA, horario, "Cliente Confirma", "11988880007")
                .andExpect(status().is3xxRedirection());
        var id = idPorHorario(cenario, SEGUNDA, horario);

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/confirmar", id).with(csrf()).session(sessao))
                .andExpect(status().is3xxRedirection());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), id);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("E2E-8: dono conclui agendamento com horário já passado — SCHEDULED vira COMPLETED (sistema-de-design-admin)")
    void e2e8ConcluirAgendamentoPassado() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);
        var horario = LocalTime.of(9, 30);
        criarManualmente(cenario, sessao, ONTEM, horario, "Cliente Concluido", "11988880008")
                .andExpect(status().is3xxRedirection());
        var id = idPorHorario(cenario, ONTEM, horario);

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/concluir", id).with(csrf()).session(sessao))
                .andExpect(status().is3xxRedirection());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), id);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().status()).isEqualTo(AppointmentStatus.COMPLETED);

        mockMvc.perform(get("/admin/agenda")
                        .session(sessao)
                        .param("professionalId", cenario.profissional().id().toString())
                        .param("date", ONTEM.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Concluído")));
    }

    @Test
    @DisplayName("E2E-9: não é possível concluir antes do horário chegar nem um agendamento já cancelado (BR-1/BR-3)")
    void e2e9NaoConcluiForaDaJanelaPermitida() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);

        // Ainda não começou.
        criarManualmente(cenario, sessao, SEGUNDA, LocalTime.of(15, 0), "Cliente Futuro", "11988880009")
                .andExpect(status().is3xxRedirection());
        var idFuturo = idPorHorario(cenario, SEGUNDA, LocalTime.of(15, 0));
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/concluir", idFuturo).with(csrf()).session(sessao))
                .andExpect(status().is3xxRedirection());
        var futuro = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), idFuturo);
        assertThat(futuro).isPresent();
        assertThat(futuro.get().status()).isEqualTo(AppointmentStatus.SCHEDULED);

        // Já cancelado.
        criarManualmente(cenario, sessao, ONTEM, LocalTime.of(16, 0), "Cliente Cancelado", "11988880010")
                .andExpect(status().is3xxRedirection());
        var idCancelado = idPorHorario(cenario, ONTEM, LocalTime.of(16, 0));
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/cancelar", idCancelado).with(csrf()).session(sessao))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/concluir", idCancelado).with(csrf()).session(sessao))
                .andExpect(status().is3xxRedirection());
        var cancelado = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), idCancelado);
        assertThat(cancelado).isPresent();
        assertThat(cancelado.get().status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName(
            "E2E-10: marcar um agendamento como fiado na agenda reflete no total em aberto do cliente (gestao-de-clientes, E2E-5)")
    void e2e10MarcarFiadoReflowNoTotalEmAberto() throws Exception {
        var cenario = semearCenario();
        var sessao = sessaoAutenticada(cenario);
        var horario = LocalTime.of(9, 0);
        criarManualmente(cenario, sessao, ONTEM, horario, "Cliente Fiado", "11988880011")
                .andExpect(status().is3xxRedirection());
        var id = idPorHorario(cenario, ONTEM, horario);
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/concluir", id).with(csrf()).session(sessao))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/pagamento", id)
                        .with(csrf())
                        .session(sessao)
                        .param("status", "ON_CREDIT"))
                .andExpect(status().is3xxRedirection());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), id);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().paymentStatus()).isEqualTo(PaymentStatus.ON_CREDIT);

        mockMvc.perform(get("/admin/clientes/{id}", agendamento.get().customerId()).session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("R$ 30,00")));
    }
}
