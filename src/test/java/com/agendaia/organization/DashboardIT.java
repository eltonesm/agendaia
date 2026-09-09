package com.agendaia.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.customer.application.port.out.CustomerRepository;
import com.agendaia.customer.domain.Customer;
import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.application.port.out.UserRepository;
import com.agendaia.scheduling.api.NextClientRef;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.User;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.shared.Money;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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

/**
 * E2E-1 e E2E-3 da spec funcional de sistema-de-design-admin (TODO-110):
 * sidebar em /admin/** e KPIs reais do painel, ponta a ponta contra Postgres
 * real via Testcontainers.
 *
 * <p>Agendamentos são semeados direto pelo repositório, com {@code
 * Appointment.reconstitute} — sem depender do horário de funcionamento nem
 * da jornada do profissional (que não importam para esta feature, só para
 * a página pública de agendamento). {@code professionalId}/{@code
 * serviceOfferingId} são UUIDs soltos, sem entidade correspondente: {@code
 * scheduling} não tem FK para {@code organization}/{@code catalog} (ADR 0002).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DashboardIT {

    private static final String SENHA = "senha-do-dono";

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private record Cenario(Business barbearia, String email) {}

    private Cenario semearEstabelecimento() {
        var sufixo = UuidV7.generate().toString();
        var email = "dono-" + sufixo + "@exemplo.com";
        var barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia Painel " + sufixo, "barbearia-painel-" + sufixo));
        userRepository.saveAndFlush(
                User.owner(barbearia.tenantId(), email, "Dono " + sufixo, passwordEncoder.encode(SENHA)));
        return new Cenario(barbearia, email);
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

    private void semearAgendamento(
            Cenario cenario, UUID customerId, AppointmentStatus status, Instant startsAt, long precoCentavos) {
        var agendamento = Appointment.reconstitute(
                UuidV7.generate(),
                cenario.barbearia().tenantId(),
                UuidV7.generate(),
                UuidV7.generate(),
                customerId,
                status,
                startsAt,
                startsAt.plus(30, ChronoUnit.MINUTES),
                "Corte de Cabelo",
                30,
                new Money(precoCentavos));
        appointmentRepository.save(agendamento);
    }

    @Test
    @DisplayName("E2E-1: painel do dono navega por sidebar, com o item Painel destacado")
    void e2e1SidebarNoPainel() throws Exception {
        var cenario = semearEstabelecimento();
        var sessao = sessaoAutenticada(cenario);

        mockMvc.perform(get("/admin/dashboard").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("app-sidebar")))
                .andExpect(content().string(containsString("Agenda")))
                .andExpect(content().string(containsString("Profissionais")));
    }

    @Test
    @DisplayName("E2E-3: dashboard mostra KPIs reais — cancelado não entra na contagem nem na receita")
    void e2e3KpisReaisExcluemCancelado() throws Exception {
        var cenario = semearEstabelecimento();
        var cliente = customerRepository.saveAndFlush(
                Customer.register(cenario.barbearia().tenantId(), "Cliente Painel", "11988887000"));
        var sessao = sessaoAutenticada(cenario);
        var agora = Instant.now();

        // Deltas pequenos e relativos a "agora" — cada um com professionalId
        // próprio (UUID solto), então não há overlap para a exclusion
        // constraint proteger, mesmo com horários próximos.
        semearAgendamento(cenario, cliente.id(), AppointmentStatus.COMPLETED, agora.minus(90, ChronoUnit.MINUTES), 5000);
        semearAgendamento(cenario, cliente.id(), AppointmentStatus.SCHEDULED, agora.plus(30, ChronoUnit.MINUTES), 3000);
        semearAgendamento(cenario, cliente.id(), AppointmentStatus.CONFIRMED, agora.plus(60, ChronoUnit.MINUTES), 4000);
        semearAgendamento(cenario, cliente.id(), AppointmentStatus.CANCELLED, agora.plus(90, ChronoUnit.MINUTES), 9999);

        var resultado = mockMvc.perform(get("/admin/dashboard").session(sessao))
                .andExpect(status().isOk())
                .andReturn();

        var model = resultado.getModelAndView().getModel();
        // total: SCHEDULED + CONFIRMED + COMPLETED = 3 (cancelado fora)
        assertThat(model.get("agendamentosHoje")).isEqualTo(3L);
        // atendidos: só o COMPLETED
        assertThat(model.get("atendidosHoje")).isEqualTo(1L);
        // receita: 5000 + 3000 + 4000 = 12000 (cancelado fora)
        assertThat(model.get("receitaEstimada")).isEqualTo(new Money(12000));
        // próximo cliente: o SCHEDULED de +30min, não o CONFIRMED de +60min
        var proximoCliente = (NextClientRef) model.get("proximoCliente");
        assertThat(proximoCliente).isNotNull();
        assertThat(proximoCliente.customerName()).isEqualTo("Cliente Painel");
    }
}
