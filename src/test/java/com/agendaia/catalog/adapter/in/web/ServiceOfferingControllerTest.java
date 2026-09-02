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

import com.agendaia.catalog.application.command.RegisterServiceOfferingCommand;
import com.agendaia.catalog.application.port.in.ListServiceOfferingsUseCase;
import com.agendaia.catalog.application.port.in.ListServicesUseCase;
import com.agendaia.catalog.application.port.in.RegisterServiceOfferingUseCase;
import com.agendaia.catalog.application.port.in.RegisteredServiceOffering;
import com.agendaia.catalog.application.port.in.ServiceOfferingView;
import com.agendaia.catalog.application.port.in.ServiceView;
import com.agendaia.catalog.domain.exception.ProfessionalNotFoundException;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.organization.api.ProfessionalRef;
import com.agendaia.platform.security.SecurityConfig;
import com.agendaia.shared.UuidV7;
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

/** Camada web isolada: os casos de uso e o ProfessionalDirectory são mock, o banco não existe. */
@WebMvcTest(ServiceOfferingController.class)
@Import(SecurityConfig.class)
@WithMockUser
class ServiceOfferingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterServiceOfferingUseCase registerServiceOffering;
    @MockitoBean private ListServiceOfferingsUseCase listServiceOfferings;
    @MockitoBean private ListServicesUseCase listServices;
    @MockitoBean private ProfessionalDirectory professionalDirectory;

    private final UUID servicoId = UuidV7.generate();
    private final UUID profissionalId = UuidV7.generate();

    @Test
    @DisplayName("GET com servico e profissional cadastrados mostra o formulario")
    void mostraFormularioQuandoHaPreRequisitos() throws Exception {
        when(listServices.list()).thenReturn(List.of(new ServiceView(servicoId, "Corte de Cabelo")));
        when(professionalDirectory.listActive())
                .thenReturn(List.of(new ProfessionalRef(profissionalId, "João da Silva")));
        when(listServiceOfferings.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/ofertas"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ofertas"))
                .andExpect(model().attributeExists("form"))
                .andExpect(content().string(Matchers.containsString("Corte de Cabelo")))
                .andExpect(content().string(Matchers.containsString("João da Silva")));
    }

    @Test
    @DisplayName("sem servico cadastrado, linka para /admin/servicos em vez de dropdown vazio (AC-1)")
    void semServicoLinkaParaTelaDeServicos() throws Exception {
        when(listServices.list()).thenReturn(List.of());
        when(professionalDirectory.listActive())
                .thenReturn(List.of(new ProfessionalRef(profissionalId, "João da Silva")));
        when(listServiceOfferings.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/ofertas"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("/admin/servicos")));
    }

    @Test
    @DisplayName("sem profissional cadastrado, linka para /admin/profissionais (AC-2)")
    void semProfissionalLinkaParaTelaDeProfissionais() throws Exception {
        when(listServices.list()).thenReturn(List.of(new ServiceView(servicoId, "Corte de Cabelo")));
        when(professionalDirectory.listActive()).thenReturn(List.of());
        when(listServiceOfferings.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/ofertas"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("/admin/profissionais")));
    }

    @Test
    @DisplayName("lista as ofertas com preco formatado via Money.format() (AC-4)")
    void listaComPrecoFormatado() throws Exception {
        when(listServices.list()).thenReturn(List.of(new ServiceView(servicoId, "Corte de Cabelo")));
        when(professionalDirectory.listActive())
                .thenReturn(List.of(new ProfessionalRef(profissionalId, "João da Silva")));
        when(listServiceOfferings.list())
                .thenReturn(List.of(new ServiceOfferingView(
                        UuidV7.generate(), "Corte de Cabelo", "João da Silva", 30, "R$ 30,00")));

        mockMvc.perform(get("/admin/ofertas"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("R$ 30,00")));
    }

    @Test
    @DisplayName("cadastro valido redireciona para a mesma tela (PRG)")
    void cadastroValido() throws Exception {
        when(registerServiceOffering.register(any(RegisterServiceOfferingCommand.class)))
                .thenReturn(new RegisteredServiceOffering(UuidV7.generate()));

        mockMvc.perform(post("/admin/ofertas")
                        .with(csrf())
                        .param("serviceId", servicoId.toString())
                        .param("professionalId", profissionalId.toString())
                        .param("durationMinutes", "30")
                        .param("price", "30.00")
                        .param("bufferMinutes", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ofertas"));
    }

    @Test
    @DisplayName("formulario invalido vira erro no campo, recarrega as listas, nao chama o caso de uso")
    void formularioInvalidoViraErroDeCampo() throws Exception {
        when(listServices.list()).thenReturn(List.of(new ServiceView(servicoId, "Corte de Cabelo")));
        when(professionalDirectory.listActive())
                .thenReturn(List.of(new ProfessionalRef(profissionalId, "João da Silva")));
        when(listServiceOfferings.list()).thenReturn(List.of());

        mockMvc.perform(post("/admin/ofertas")
                        .with(csrf())
                        .param("durationMinutes", "0")
                        .param("bufferMinutes", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ofertas"))
                .andExpect(model().attributeHasFieldErrors("form", "serviceId", "professionalId", "price"));

        verify(registerServiceOffering, never()).register(any());
    }

    @Test
    @DisplayName("profissional de outro tenant (BR-8) vira erro no campo, nao 500 (AC-3)")
    void profissionalInvalidoViraErroDeCampo() throws Exception {
        when(registerServiceOffering.register(any(RegisterServiceOfferingCommand.class)))
                .thenThrow(new ProfessionalNotFoundException());
        when(listServices.list()).thenReturn(List.of(new ServiceView(servicoId, "Corte de Cabelo")));
        when(professionalDirectory.listActive())
                .thenReturn(List.of(new ProfessionalRef(profissionalId, "João da Silva")));
        when(listServiceOfferings.list()).thenReturn(List.of());

        mockMvc.perform(post("/admin/ofertas")
                        .with(csrf())
                        .param("serviceId", servicoId.toString())
                        .param("professionalId", profissionalId.toString())
                        .param("durationMinutes", "30")
                        .param("price", "30.00")
                        .param("bufferMinutes", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ofertas"))
                .andExpect(model().attributeHasFieldErrors("form", "professionalId"));
    }

    @Test
    @DisplayName("POST sem token CSRF e recusado")
    void semCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/ofertas")
                        .param("serviceId", servicoId.toString())
                        .param("professionalId", profissionalId.toString())
                        .param("durationMinutes", "30")
                        .param("price", "30.00")
                        .param("bufferMinutes", "0"))
                .andExpect(status().isForbidden());

        verify(registerServiceOffering, never()).register(any());
    }

    @Test
    @DisplayName("sem sessao, a rota manda para o login")
    void semSessaoExigeLogin() throws Exception {
        mockMvc.perform(get("/admin/ofertas").with(anonymous())).andExpect(status().is3xxRedirection());
    }
}
