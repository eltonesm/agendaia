package com.agendaia.organization.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.agendaia.organization.application.command.RegisterProfessionalCommand;
import com.agendaia.organization.application.port.in.ListProfessionalsUseCase;
import com.agendaia.organization.application.port.in.ProfessionalView;
import com.agendaia.organization.application.port.in.RegisterProfessionalUseCase;
import com.agendaia.organization.application.port.in.RegisteredProfessional;
import com.agendaia.organization.adapter.in.web.request.RegisterProfessionalRequest;
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

/**
 * Camada web isolada: os dois casos de uso são mock, o banco não existe.
 *
 * <p>{@code @WithMockUser(roles = "OWNER")} basta aqui — a rota só precisa
 * de uma sessão autenticada com a authority que {@code /admin/**} exige
 * (BR-6/E2E-5, back-office-operador), e nenhum teste consulta o tipo do
 * principal (ao contrário do cadastro, que autentica programaticamente).
 */
@WebMvcTest(ProfessionalController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "OWNER")
class ProfessionalControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterProfessionalUseCase registerProfessional;
    @MockitoBean private ListProfessionalsUseCase listProfessionals;

    @Test
    @DisplayName("GET lista os profissionais e mostra o formulario vazio")
    void listaEFormularioVazio() throws Exception {
        when(listProfessionals.list())
                .thenReturn(List.of(new ProfessionalView(UuidV7.generate(), "João da Silva")));

        mockMvc.perform(get("/admin/profissionais"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/profissionais"))
                .andExpect(model().attributeExists("form"))
                .andExpect(content().string(Matchers.containsString("João da Silva")));
    }

    @Test
    @DisplayName("estabelecimento sem profissional mostra a lista vazia, nao erro")
    void listaVazia() throws Exception {
        when(listProfessionals.list()).thenReturn(List.of());

        mockMvc.perform(get("/admin/profissionais"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Nenhum profissional")));
    }

    @Test
    @DisplayName("cadastro valido redireciona para a mesma tela (PRG)")
    void cadastroValido() throws Exception {
        when(registerProfessional.register(any(RegisterProfessionalCommand.class)))
                .thenReturn(new RegisteredProfessional(UuidV7.generate(), "João da Silva"));

        mockMvc.perform(post("/admin/profissionais").with(csrf()).param("name", "João da Silva"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profissionais"));
    }

    @Test
    @DisplayName("nome vazio vira erro no campo, recarrega a lista, nao chama o caso de uso")
    void nomeVazioViraErroDeCampo() throws Exception {
        when(listProfessionals.list())
                .thenReturn(List.of(new ProfessionalView(UuidV7.generate(), "Já Cadastrado")));

        mockMvc.perform(post("/admin/profissionais").with(csrf()).param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/profissionais"))
                .andExpect(model().attributeHasFieldErrors("form", "name"))
                // A lista já cadastrada continua visível — não é só o erro do formulário.
                .andExpect(content().string(Matchers.containsString("Já Cadastrado")));

        verify(registerProfessional, never()).register(any());
    }

    @Test
    @DisplayName("POST sem token CSRF e recusado")
    void semCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/profissionais").param("name", "João da Silva"))
                .andExpect(status().isForbidden());

        verify(registerProfessional, never()).register(any());
    }

    @Test
    @DisplayName("sem sessao, a rota manda para o login")
    void semSessaoExigeLogin() throws Exception {
        mockMvc.perform(get("/admin/profissionais").with(anonimo()))
                .andExpect(status().is3xxRedirection());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor anonimo() {
        return org.springframework.security.test.web.servlet.request
                .SecurityMockMvcRequestPostProcessors.anonymous();
    }
}
