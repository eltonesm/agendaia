package com.agendaia.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository;
import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.TimeOffRepository;
import com.agendaia.organization.application.port.out.UserRepository;
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.User;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
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
 * E2E-1 e E2E-2 da spec funcional de horario-jornada-bloqueios: cadastro
 * completo dos três agregados, e sobreposição de jornada recusada.
 *
 * <p>Postgres real via Testcontainers. Sem {@code @Transactional}, mesmo
 * motivo das ITs anteriores. Ordem de limpeza: {@code time_off} e
 * {@code work_schedule} antes de {@code professional} (FK), depois
 * {@code business_operating_hours}, {@code professional}, {@code user},
 * {@code business}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HorarioJornadaBloqueioRegistrationIT {

    private static final String EMAIL = "dono@exemplo.com";
    private static final String SENHA = "senha-do-dono";

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private BusinessOperatingHoursRepository businessOperatingHoursRepository;
    @Autowired private WorkScheduleRepository workScheduleRepository;
    @Autowired private TimeOffRepository timeOffRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Business barbearia;
    private Professional maria;

    @BeforeEach
    void semear() {
        timeOffRepository.deleteAllInBatch();
        workScheduleRepository.deleteAllInBatch();
        businessOperatingHoursRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();

        barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia do João", "barbearia-do-joao-horarios"));
        userRepository.saveAndFlush(
                User.owner(barbearia.tenantId(), EMAIL, "João", passwordEncoder.encode(SENHA)));
        maria = professionalRepository.saveAndFlush(
                Professional.register(barbearia.tenantId(), "Maria Oliveira"));
    }

    private MockHttpSession sessaoAutenticada() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", EMAIL)
                        .param("password", SENHA))
                .andReturn()
                .getRequest()
                .getSession();
    }

    @Test
    @DisplayName("E2E-1: cadastro completo dos tres agregados, caminho feliz")
    void cadastroCompleto() throws Exception {
        var sessao = sessaoAutenticada();

        mockMvc.perform(post("/admin/horario-funcionamento")
                        .with(csrf())
                        .session(sessao)
                        .param("dayOfWeek", "MONDAY")
                        .param("opensAt", "08:00")
                        .param("closesAt", "18:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/horario-funcionamento"));

        mockMvc.perform(post("/admin/jornadas")
                        .with(csrf())
                        .session(sessao)
                        .param("professionalId", maria.id().toString())
                        .param("dayOfWeek", "MONDAY")
                        .param("startsAt", "08:00")
                        .param("endsAt", "12:00"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/jornadas")
                        .with(csrf())
                        .session(sessao)
                        .param("professionalId", maria.id().toString())
                        .param("dayOfWeek", "MONDAY")
                        .param("startsAt", "13:00")
                        .param("endsAt", "18:00"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/bloqueios")
                        .with(csrf())
                        .session(sessao)
                        .param("professionalId", maria.id().toString())
                        .param("startsAt", "2026-09-10T09:00")
                        .param("endsAt", "2026-09-10T18:00")
                        .param("reason", "Consulta médica"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/bloqueios")
                        .with(csrf())
                        .session(sessao)
                        .param("professionalId", "")
                        .param("startsAt", "2026-12-25T00:00")
                        .param("endsAt", "2026-12-26T00:00")
                        .param("reason", "Natal"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/horario-funcionamento").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("segunda-feira")));

        mockMvc.perform(get("/admin/jornadas").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Maria Oliveira")));

        mockMvc.perform(get("/admin/bloqueios").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Consulta médica")))
                .andExpect(content().string(Matchers.containsString("Estabelecimento inteiro")));

        assertThat(businessOperatingHoursRepository.findByTenantIdAndActiveTrueOrderByDayOfWeekAscOpensAtAsc(
                        barbearia.id()))
                .hasSize(1);
        assertThat(workScheduleRepository.findByTenantIdAndActiveTrueOrderByProfessionalIdAscDayOfWeekAscStartsAtAsc(
                        barbearia.id()))
                .hasSize(2);
        assertThat(timeOffRepository.findByTenantIdAndActiveTrueOrderByStartsAtDesc(barbearia.id())).hasSize(2);
    }

    @Test
    @DisplayName("E2E-2: faixa de jornada sobreposta e recusada, sem gravar")
    void faixaSobrepostaERecusada() throws Exception {
        var sessao = sessaoAutenticada();

        mockMvc.perform(post("/admin/jornadas")
                        .with(csrf())
                        .session(sessao)
                        .param("professionalId", maria.id().toString())
                        .param("dayOfWeek", "MONDAY")
                        .param("startsAt", "08:00")
                        .param("endsAt", "12:00"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/jornadas")
                        .with(csrf())
                        .session(sessao)
                        .param("professionalId", maria.id().toString())
                        .param("dayOfWeek", "MONDAY")
                        .param("startsAt", "10:00")
                        .param("endsAt", "14:00"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "startsAt"));

        assertThat(workScheduleRepository.findByTenantIdAndActiveTrueOrderByProfessionalIdAscDayOfWeekAscStartsAtAsc(
                        barbearia.id()))
                .hasSize(1);
    }
}
