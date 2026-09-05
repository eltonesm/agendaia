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

import com.agendaia.organization.application.command.RegisterTimeOffCommand;
import com.agendaia.organization.application.port.in.ListProfessionalsUseCase;
import com.agendaia.organization.application.port.in.ListTimeOffUseCase;
import com.agendaia.organization.application.port.in.ProfessionalView;
import com.agendaia.organization.application.port.in.RegisterTimeOffUseCase;
import com.agendaia.organization.application.port.in.RegisteredTimeOff;
import com.agendaia.organization.application.port.in.TimeOffView;
import com.agendaia.organization.domain.exception.ProfessionalNotFoundException;
import com.agendaia.platform.security.SecurityConfig;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
@WebMvcTest(TimeOffController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "OWNER")
class TimeOffControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterTimeOffUseCase registerTimeOff;
    @MockitoBean private ListTimeOffUseCase listTimeOff;
    @MockitoBean private ListProfessionalsUseCase listProfessionals;

    private final UUID professionalId = UuidV7.generate();

    @Test
    @DisplayName("GET mostra o formulario mesmo sem nenhum profissional cadastrado")
    void mostraFormularioMesmoSemProfissional() throws Exception {
        when(listProfessionals.list()).thenReturn(List.of());
        when(listTimeOff.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/bloqueios"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bloqueios"))
                .andExpect(model().attributeExists("form"))
                .andExpect(content().string(Matchers.containsString("Todos os profissionais")));
    }

    @Test
    @DisplayName("lista distingue bloqueio de profissional do que vale para todos")
    void listaDistingueBloqueioGeral() throws Exception {
        when(listProfessionals.list()).thenReturn(List.of());
        when(listTimeOff.list())
                .thenReturn(List.of(new TimeOffView(
                        UuidV7.generate(), null, Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS), "Natal")));

        mockMvc.perform(get("/admin/bloqueios"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Estabelecimento inteiro")));
    }

    @Test
    @DisplayName("cadastro de bloqueio geral (sem profissional) redireciona para a mesma tela (PRG)")
    void cadastroDeBloqueioGeral() throws Exception {
        when(registerTimeOff.register(any(RegisterTimeOffCommand.class)))
                .thenReturn(new RegisteredTimeOff(UuidV7.generate()));

        mockMvc.perform(post("/admin/bloqueios")
                        .with(csrf())
                        .param("professionalId", "")
                        .param("startsAt", "2026-12-25T00:00")
                        .param("endsAt", "2026-12-26T00:00")
                        .param("reason", "Natal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bloqueios"));
    }

    @Test
    @DisplayName("cadastro de bloqueio de um profissional redireciona para a mesma tela (PRG)")
    void cadastroDeBloqueioDeProfissional() throws Exception {
        when(registerTimeOff.register(any(RegisterTimeOffCommand.class)))
                .thenReturn(new RegisteredTimeOff(UuidV7.generate()));

        mockMvc.perform(post("/admin/bloqueios")
                        .with(csrf())
                        .param("professionalId", professionalId.toString())
                        .param("startsAt", "2026-09-10T09:00")
                        .param("endsAt", "2026-09-10T18:00")
                        .param("reason", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bloqueios"));
    }

    @Test
    @DisplayName("profissional de outro tenant (BR-8) vira erro no campo, nao 500")
    void profissionalInvalidoViraErroDeCampo() throws Exception {
        when(registerTimeOff.register(any(RegisterTimeOffCommand.class))).thenThrow(new ProfessionalNotFoundException());
        when(listProfessionals.list()).thenReturn(List.of(new ProfessionalView(professionalId, "João da Silva")));
        when(listTimeOff.list()).thenReturn(List.of());

        mockMvc.perform(post("/admin/bloqueios")
                        .with(csrf())
                        .param("professionalId", professionalId.toString())
                        .param("startsAt", "2026-09-10T09:00")
                        .param("endsAt", "2026-09-10T18:00")
                        .param("reason", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bloqueios"))
                .andExpect(model().attributeHasFieldErrors("form", "professionalId"));
    }

    @Test
    @DisplayName("fim antes ou igual ao inicio vira erro no campo, nao chama o caso de uso")
    void intervaloInvalidoViraErroDeCampo() throws Exception {
        when(listProfessionals.list()).thenReturn(List.of());
        when(listTimeOff.list()).thenReturn(List.of());

        mockMvc.perform(post("/admin/bloqueios")
                        .with(csrf())
                        .param("professionalId", "")
                        .param("startsAt", "2026-09-10T18:00")
                        .param("endsAt", "2026-09-10T09:00")
                        .param("reason", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bloqueios"));

        verify(registerTimeOff, never()).register(any());
    }

    @Test
    @DisplayName("POST sem token CSRF e recusado")
    void semCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/bloqueios")
                        .param("professionalId", "")
                        .param("startsAt", "2026-12-25T00:00")
                        .param("endsAt", "2026-12-26T00:00")
                        .param("reason", ""))
                .andExpect(status().isForbidden());

        verify(registerTimeOff, never()).register(any());
    }

    @Test
    @DisplayName("sem sessao, a rota manda para o login")
    void semSessaoExigeLogin() throws Exception {
        mockMvc.perform(get("/admin/bloqueios").with(anonymous())).andExpect(status().is3xxRedirection());
    }
}
