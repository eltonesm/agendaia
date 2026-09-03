package com.agendaia.organization.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.agendaia.organization.application.command.RegisterWorkScheduleCommand;
import com.agendaia.organization.application.port.in.ListProfessionalsUseCase;
import com.agendaia.organization.application.port.in.ListWorkSchedulesUseCase;
import com.agendaia.organization.application.port.in.ProfessionalView;
import com.agendaia.organization.application.port.in.RegisterWorkScheduleUseCase;
import com.agendaia.organization.application.port.in.RegisteredWorkSchedule;
import com.agendaia.organization.application.port.in.WorkScheduleView;
import com.agendaia.organization.domain.exception.WorkScheduleOverlapException;
import com.agendaia.platform.security.SecurityConfig;
import com.agendaia.shared.UuidV7;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Camada web isolada: os casos de uso são mock, o banco não existe. */
@WebMvcTest(WorkScheduleController.class)
@Import(SecurityConfig.class)
@WithMockUser
class WorkScheduleControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterWorkScheduleUseCase registerWorkSchedule;
    @MockitoBean private ListWorkSchedulesUseCase listWorkSchedules;
    @MockitoBean private ListProfessionalsUseCase listProfessionals;

    private final UUID professionalId = UuidV7.generate();

    @Test
    @DisplayName("GET com profissional cadastrado mostra o formulario")
    void mostraFormularioQuandoHaProfissional() throws Exception {
        when(listProfessionals.list()).thenReturn(List.of(new ProfessionalView(professionalId, "João da Silva")));
        when(listWorkSchedules.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/jornadas"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/jornadas"))
                .andExpect(model().attributeExists("form"))
                .andExpect(content().string(Matchers.containsString("João da Silva")));
    }

    @Test
    @DisplayName("sem profissional cadastrado, linka para /admin/profissionais em vez de dropdown vazio (AC-1)")
    void semProfissionalLinkaParaTelaDeProfissionais() throws Exception {
        when(listProfessionals.list()).thenReturn(List.of());
        when(listWorkSchedules.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/jornadas"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("/admin/profissionais")));
    }

    @Test
    @DisplayName("lista as faixas com nome do profissional resolvido")
    void listaComNomeResolvido() throws Exception {
        when(listProfessionals.list()).thenReturn(List.of(new ProfessionalView(professionalId, "João da Silva")));
        when(listWorkSchedules.list())
                .thenReturn(List.of(new WorkScheduleView(
                        UuidV7.generate(), "João da Silva", DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0))));

        mockMvc.perform(get("/admin/jornadas"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("João da Silva")))
                .andExpect(content().string(Matchers.containsString("segunda-feira")));
    }

    @Test
    @DisplayName("cadastro valido redireciona para a mesma tela (PRG)")
    void cadastroValido() throws Exception {
        when(registerWorkSchedule.register(any(RegisterWorkScheduleCommand.class)))
                .thenReturn(new RegisteredWorkSchedule(UuidV7.generate()));

        mockMvc.perform(post("/admin/jornadas")
                        .with(csrf())
                        .param("professionalId", professionalId.toString())
                        .param("dayOfWeek", "MONDAY")
                        .param("startsAt", "08:00")
                        .param("endsAt", "12:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/jornadas"));
    }

    @Test
    @DisplayName("faixa sobreposta (BR-3) vira erro no campo, nao 500")
    void faixaSobrepostaViraErroDeCampo() throws Exception {
        when(registerWorkSchedule.register(any(RegisterWorkScheduleCommand.class)))
                .thenThrow(new WorkScheduleOverlapException());
        when(listProfessionals.list()).thenReturn(List.of(new ProfessionalView(professionalId, "João da Silva")));
        when(listWorkSchedules.list()).thenReturn(List.of());

        mockMvc.perform(post("/admin/jornadas")
                        .with(csrf())
                        .param("professionalId", professionalId.toString())
                        .param("dayOfWeek", "MONDAY")
                        .param("startsAt", "10:00")
                        .param("endsAt", "14:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/jornadas"))
                .andExpect(model().attributeHasFieldErrors("form", "startsAt"));
    }

    @Test
    @DisplayName("POST sem token CSRF e recusado")
    void semCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/jornadas")
                        .param("professionalId", professionalId.toString())
                        .param("dayOfWeek", "MONDAY")
                        .param("startsAt", "08:00")
                        .param("endsAt", "12:00"))
                .andExpect(status().isForbidden());

        verify(registerWorkSchedule, never()).register(any());
    }

    @Test
    @DisplayName("sem sessao, a rota manda para o login")
    void semSessaoExigeLogin() throws Exception {
        mockMvc.perform(get("/admin/jornadas").with(anonymous())).andExpect(status().is3xxRedirection());
    }
}
