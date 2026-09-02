package com.agendaia.catalog.adapter.in.web;

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

import com.agendaia.catalog.application.command.RegisterServiceCommand;
import com.agendaia.catalog.application.port.in.ListServicesUseCase;
import com.agendaia.catalog.application.port.in.RegisterServiceUseCase;
import com.agendaia.catalog.application.port.in.RegisteredService;
import com.agendaia.catalog.application.port.in.ServiceView;
import com.agendaia.catalog.domain.exception.ServiceNameAlreadyUsedException;
import com.agendaia.platform.security.SecurityConfig;
import com.agendaia.shared.UuidV7;
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
@WebMvcTest(ServiceController.class)
@Import(SecurityConfig.class)
@WithMockUser
class ServiceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterServiceUseCase registerService;
    @MockitoBean private ListServicesUseCase listServices;

    @Test
    @DisplayName("GET lista os serviços e mostra o formulario vazio")
    void listaEFormularioVazio() throws Exception {
        when(listServices.list()).thenReturn(List.of(new ServiceView(UuidV7.generate(), "Corte de Cabelo")));

        mockMvc.perform(get("/admin/servicos"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/servicos"))
                .andExpect(model().attributeExists("form"))
                .andExpect(content().string(Matchers.containsString("Corte de Cabelo")));
    }

    @Test
    @DisplayName("estabelecimento sem servico mostra a lista vazia, nao erro")
    void listaVazia() throws Exception {
        when(listServices.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/servicos"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Nenhum serviço")));
    }

    @Test
    @DisplayName("cadastro valido redireciona para a mesma tela (PRG)")
    void cadastroValido() throws Exception {
        when(registerService.register(any(RegisterServiceCommand.class)))
                .thenReturn(new RegisteredService(UuidV7.generate(), "Corte de Cabelo"));

        mockMvc.perform(post("/admin/servicos").with(csrf()).param("name", "Corte de Cabelo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/servicos"));
    }

    @Test
    @DisplayName("nome vazio vira erro no campo, recarrega a lista, nao chama o caso de uso")
    void nomeVazioViraErroDeCampo() throws Exception {
        when(listServices.list()).thenReturn(List.of(new ServiceView(UuidV7.generate(), "Já Cadastrado")));

        mockMvc.perform(post("/admin/servicos").with(csrf()).param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/servicos"))
                .andExpect(model().attributeHasFieldErrors("form", "name"))
                .andExpect(content().string(Matchers.containsString("Já Cadastrado")));

        verify(registerService, never()).register(any());
    }

    @Test
    @DisplayName("nome duplicado (BR-1) vira erro no campo, nao 500")
    void nomeDuplicadoViraErroDeCampo() throws Exception {
        when(registerService.register(any(RegisterServiceCommand.class)))
                .thenThrow(new ServiceNameAlreadyUsedException());
        when(listServices.list()).thenReturn(List.of(new ServiceView(UuidV7.generate(), "Corte de Cabelo")));

        mockMvc.perform(post("/admin/servicos").with(csrf()).param("name", "Corte de Cabelo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/servicos"))
                .andExpect(model().attributeHasFieldErrors("form", "name"));
    }

    @Test
    @DisplayName("POST sem token CSRF e recusado")
    void semCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/servicos").param("name", "Corte de Cabelo"))
                .andExpect(status().isForbidden());

        verify(registerService, never()).register(any());
    }

    @Test
    @DisplayName("sem sessao, a rota manda para o login")
    void semSessaoExigeLogin() throws Exception {
        mockMvc.perform(get("/admin/servicos").with(anonymous())).andExpect(status().is3xxRedirection());
    }
}
