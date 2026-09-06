package com.agendaia.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessOperatingHours;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.WorkSchedule;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.shared.Money;
import com.agendaia.shared.UuidV7;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
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
 * E2E-1 a E2E-7 da spec funcional de confirmacao-e-cancelamento, ponta a
 * ponta contra Postgres real via Testcontainers — obrigatório para a
 * exclusion constraint do ADR 0005 (liberação de horário ao cancelar).
 *
 * <p>Cada teste cria seu próprio estabelecimento com slug único, mesma
 * técnica de {@code PaginaPublicaAgendamentoIT} (TODO-006).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConfirmacaoECancelamentoIT {

    private static final DayOfWeek DIA = DayOfWeek.MONDAY;
    private static final LocalDate SEGUNDA = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private static final LocalDate ONTEM = LocalDate.now().minusDays(1);

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private ServiceOfferingRepository serviceOfferingRepository;
    @Autowired private BusinessOperatingHoursRepository businessOperatingHoursRepository;
    @Autowired private WorkScheduleRepository workScheduleRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private record Cenario(Business barbearia, Professional profissional, ServiceOffering oferta) {}

    private Cenario semearCenario() {
        return semearCenario(null);
    }

    private Cenario semearCenario(String whatsapp) {
        var sufixo = UuidV7.generate().toString();
        var barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia E2E " + sufixo, "barbearia-e2e-" + sufixo, whatsapp));
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

    private static RequestPostProcessor ip(String endereco) {
        return request -> {
            request.setRemoteAddr(endereco);
            return request;
        };
    }

    /** Agenda via o fluxo HTTP de verdade (TODO-006) — sem checagem de horizonte no POST, aceita data passada. */
    private ResultActions agendar(
            Cenario cenario, LocalDate data, LocalTime horario, String name, String phone, RequestPostProcessor ip)
            throws Exception {
        return mockMvc.perform(post("/b/{slug}/ofertas/{offeringId}", cenario.barbearia().slug(), cenario.oferta().id())
                .with(csrf())
                .with(ip)
                .param("startsAt", LocalDateTime.of(data, horario).toString())
                .param("data", data.toString())
                .param("name", name)
                .param("phone", phone)
                .param("website", ""));
    }

    private UUID idDoRedirect(ResultActions resultado) throws Exception {
        var url = resultado.andReturn().getResponse().getRedirectedUrl();
        return UUID.fromString(url.substring(url.lastIndexOf('/') + 1));
    }

    private ResultActions confirmar(Cenario cenario, UUID appointmentId, RequestPostProcessor ip) throws Exception {
        return mockMvc.perform(post("/b/{slug}/agendamentos/{id}/confirmar", cenario.barbearia().slug(), appointmentId)
                .with(csrf())
                .with(ip));
    }

    private ResultActions cancelar(Cenario cenario, UUID appointmentId, RequestPostProcessor ip) throws Exception {
        return mockMvc.perform(post("/b/{slug}/agendamentos/{id}/cancelar", cenario.barbearia().slug(), appointmentId)
                .with(csrf())
                .with(ip));
    }

    private ResultActions ver(Cenario cenario, UUID appointmentId) throws Exception {
        return mockMvc.perform(get("/b/{slug}/agendamentos/{id}", cenario.barbearia().slug(), appointmentId));
    }

    @Test
    @DisplayName("E2E-1: caminho feliz — confirmar presença muda SCHEDULED para CONFIRMED")
    void e2e1ConfirmarPresenca() throws Exception {
        var cenario = semearCenario();
        var id = idDoRedirect(agendar(cenario, SEGUNDA, LocalTime.of(8, 0), "Joao", "11988880001", ip("10.2.0.1")));

        confirmar(cenario, id, ip("10.2.0.1")).andExpect(status().is3xxRedirection());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), id);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("E2E-2: cancelamento libera o horário para outra reserva")
    void e2e2CancelamentoLiberaHorario() throws Exception {
        var cenario = semearCenario();
        var horario = LocalTime.of(9, 0);
        var id = idDoRedirect(agendar(cenario, SEGUNDA, horario, "Joao", "11988880002", ip("10.2.0.2")));

        cancelar(cenario, id, ip("10.2.0.2")).andExpect(status().is3xxRedirection());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), id);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().status()).isEqualTo(AppointmentStatus.CANCELLED);

        assertThat(appointmentRepository.findOccupiedRanges(
                        cenario.barbearia().tenantId(), cenario.profissional().id(), SEGUNDA))
                .isEmpty();

        // Uma nova reserva para o MESMO horário é aceita depois do cancelamento.
        agendar(cenario, SEGUNDA, horario, "Maria", "11988880022", ip("10.2.0.23"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("E2E-3: token de um tenant não abre nem altera agendamento de outro")
    void e2e3IsolamentoEntreTenants() throws Exception {
        var cenarioA = semearCenario();
        var cenarioB = semearCenario();
        var idDoB = idDoRedirect(agendar(cenarioB, SEGUNDA, LocalTime.of(10, 0), "Cliente B", "11988880003", ip("10.2.0.3")));

        mockMvc.perform(get("/b/{slug}/agendamentos/{id}", cenarioA.barbearia().slug(), idDoB))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/b/{slug}/agendamentos/{id}/cancelar", cenarioA.barbearia().slug(), idDoB)
                        .with(csrf())
                        .with(ip("10.2.0.31")))
                .andExpect(status().isNotFound());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenarioB.barbearia().tenantId(), idDoB);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().status()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("E2E-4: cancelar duas vezes é idempotente — sem erro, sem gravação nova")
    void e2e4AcoesIdempotentes() throws Exception {
        var cenario = semearCenario();
        var id = idDoRedirect(agendar(cenario, SEGUNDA, LocalTime.of(11, 0), "Joao", "11988880004", ip("10.2.0.4")));

        cancelar(cenario, id, ip("10.2.0.4")).andExpect(status().is3xxRedirection());
        cancelar(cenario, id, ip("10.2.0.41")).andExpect(status().is3xxRedirection());
        confirmar(cenario, id, ip("10.2.0.42")).andExpect(status().is3xxRedirection());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), id);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("E2E-5: agendamento no passado é somente leitura — confirmar/cancelar não têm efeito")
    void e2e5AgendamentoNoPassadoSomenteLeitura() throws Exception {
        var cenario = semearCenario();
        var id = idDoRedirect(
                agendar(cenario, ONTEM, LocalTime.of(9, 0), "Joao", "11988880005", ip("10.2.0.5")));

        ver(cenario, id)
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Confirmar presença"))))
                .andExpect(content().string(not(containsString("Cancelar"))));

        confirmar(cenario, id, ip("10.2.0.51")).andExpect(status().is3xxRedirection());
        cancelar(cenario, id, ip("10.2.0.52")).andExpect(status().is3xxRedirection());

        var agendamento = appointmentRepository.findByTenantIdAndId(cenario.barbearia().tenantId(), id);
        assertThat(agendamento).isPresent();
        assertThat(agendamento.get().status()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("E2E-6: teto de 3 agendamentos futuros conta SCHEDULED e CONFIRMED juntos")
    void e2e6TetoContaConfirmed() throws Exception {
        var cenario = semearCenario();
        var telefone = "11988880006";

        var id1 = idDoRedirect(
                agendar(cenario, SEGUNDA, LocalTime.of(8, 0), "Cliente Teto", telefone, ip("10.2.0.60")));
        confirmar(cenario, id1, ip("10.2.0.61")); // 1 CONFIRMED

        agendar(cenario, SEGUNDA, LocalTime.of(8, 30), "Cliente Teto", telefone, ip("10.2.0.62"))
                .andExpect(status().is3xxRedirection()); // 2 ativos (1 CONFIRMED + 1 SCHEDULED)
        agendar(cenario, SEGUNDA, LocalTime.of(9, 0), "Cliente Teto", telefone, ip("10.2.0.63"))
                .andExpect(status().is3xxRedirection()); // 3 ativos

        // Teto atingido — sem o fix da BR-6, o 1o CONFIRMED não contaria, e este 4o passaria.
        agendar(cenario, SEGUNDA, LocalTime.of(9, 30), "Cliente Teto", telefone, ip("10.2.0.64"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("máximo de agendamentos")));
    }

    @Test
    @DisplayName("E2E-7: link wa.me aparece só quando o estabelecimento tem WhatsApp cadastrado")
    void e2e7WhatsappCondicional() throws Exception {
        var comWhatsapp = semearCenario("5511988880077");
        var semWhatsapp = semearCenario(null);

        var idComWhatsapp = idDoRedirect(
                agendar(comWhatsapp, SEGUNDA, LocalTime.of(8, 0), "Joao", "11988880071", ip("10.2.0.71")));
        var idSemWhatsapp = idDoRedirect(
                agendar(semWhatsapp, SEGUNDA, LocalTime.of(8, 0), "Joao", "11988880072", ip("10.2.0.72")));

        ver(comWhatsapp, idComWhatsapp)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wa.me/5511988880077")));

        ver(semWhatsapp, idSemWhatsapp)
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("wa.me"))));
    }
}
