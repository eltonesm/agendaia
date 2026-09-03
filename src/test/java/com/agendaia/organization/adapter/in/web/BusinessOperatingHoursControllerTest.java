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

import com.agendaia.organization.application.command.RegisterBusinessOperatingHoursCommand;
import com.agendaia.organization.application.port.in.BusinessOperatingHoursView;
import com.agendaia.organization.application.port.in.ListBusinessOperatingHoursUseCase;
import com.agendaia.organization.application.port.in.RegisterBusinessOperatingHoursUseCase;
import com.agendaia.organization.application.port.in.RegisteredBusinessOperatingHours;
import com.agendaia.platform.security.SecurityConfig;
import com.agendaia.shared.UuidV7;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Camada web isolada: os dois casos de uso são mock, o banco não existe. */
@WebMvcTest(BusinessOperatingHoursController.class)
@Import(SecurityConfig.class)
@WithMockUser
class BusinessOperatingHoursControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterBusinessOperatingHoursUseCase registerBusinessOperatingHours;
    @MockitoBean private ListBusinessOperatingHoursUseCase listBusinessOperatingHours;

    @Test
    @DisplayName("GET lista as faixas e mostra o formulario vazio")
    void listaEFormularioVazio() throws Exception {
        when(listBusinessOperatingHours.list())
                .thenReturn(List.of(new BusinessOperatingHoursView(
                        UuidV7.generate(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0))));

        mockMvc.perform(get("/admin/horario-funcionamento"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/horario-funcionamento"))
                .andExpect(model().attributeExists("form"))
                .andExpect(content().string(Matchers.containsString("segunda-feira")));
    }

    @Test
    @DisplayName("estabelecimento sem faixa mostra a lista vazia, nao erro")
    void listaVazia() throws Exception {
        when(listBusinessOperatingHours.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/horario-funcionamento"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Nenhuma faixa")));
    }

    @Test
    @DisplayName("cadastro valido redireciona para a mesma tela (PRG)")
    void cadastroValido() throws Exception {
        when(registerBusinessOperatingHours.register(any(RegisterBusinessOperatingHoursCommand.class)))
                .thenReturn(new RegisteredBusinessOperatingHours(UuidV7.generate()));

        mockMvc.perform(post("/admin/horario-funcionamento")
                        .with(csrf())
                        .param("dayOfWeek", "MONDAY")
                        .param("opensAt", "08:00")
                        .param("closesAt", "18:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/horario-funcionamento"));
    }

    @Test
    @DisplayName("fechamento antes ou igual a abertura vira erro no campo, nao chama o caso de uso")
    void fechamentoInvalidoViraErroDeCampo() throws Exception {
        when(listBusinessOperatingHours.list()).thenReturn(List.of());

        mockMvc.perform(post("/admin/horario-funcionamento")
                        .with(csrf())
                        .param("dayOfWeek", "MONDAY")
                        .param("opensAt", "18:00")
                        .param("closesAt", "08:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/horario-funcionamento"));

        verify(registerBusinessOperatingHours, never()).register(any());
    }

    @Test
    @DisplayName("POST sem token CSRF e recusado")
    void semCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/horario-funcionamento")
                        .param("dayOfWeek", "MONDAY")
                        .param("opensAt", "08:00")
                        .param("closesAt", "18:00"))
                .andExpect(status().isForbidden());

        verify(registerBusinessOperatingHours, never()).register(any());
    }

    @Test
    @DisplayName("sem sessao, a rota manda para o login")
    void semSessaoExigeLogin() throws Exception {
        mockMvc.perform(get("/admin/horario-funcionamento").with(anonymous())).andExpect(status().is3xxRedirection());
    }
}
