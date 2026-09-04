package com.agendaia.organization.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.agendaia.organization.application.command.RegisterBusinessCommand;
import com.agendaia.organization.application.port.in.RegisterBusinessUseCase;
import com.agendaia.organization.application.port.in.RegisteredBusiness;
import com.agendaia.organization.domain.exception.EmailAlreadyUsedException;
import com.agendaia.organization.domain.exception.SlugUnavailableException;
import com.agendaia.organization.adapter.in.web.request.RegistrationRequest;
import com.agendaia.platform.security.AuthenticatedUser;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.agendaia.platform.security.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/** Camada web isolada: o caso de uso é mock, o banco não existe. */
@WebMvcTest(RegistrationController.class)
// @WebMvcTest nao carrega @Configuration comum: sem este Import valeria a
// cadeia padrao do Spring Security, que exige autenticacao para tudo e
// devolveria 401 ate no GET do formulario publico.
@Import(SecurityConfig.class)
class RegistrationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterBusinessUseCase registerBusiness;

    // Nome explícito: RegistrationController agora pede o UserDetailsService
    // por @Qualifier("businessUserDetailsService") -- desde que a conta do
    // operador existe, há dois beans desse tipo no contexto completo
    // (back-office-operador, TODO-009), e a injeção por tipo deixou de ser
    // suficiente.
    @MockitoBean(name = "businessUserDetailsService")
    private UserDetailsService userDetailsService;
    // Espiao, nao mock: o mock devolveria null em loadDeferredContext e o
    // SecurityContextHolderFilter exige um Supplier nao-nulo — a propria
    // cadeia de filtros quebraria. Com spy o comportamento e real e a
    // verificacao de saveContext continua possivel.
    @MockitoSpyBean private SecurityContextRepository securityContextRepository;

    private RegisteredBusiness registrado() {
        var id = UuidV7.generate();
        return new RegisteredBusiness(
                id, TenantId.of(id), "Barbearia do João", "barbearia-do-joao", "joao@exemplo.com");
    }

    private AuthenticatedUser principal() {
        var id = UuidV7.generate();
        return new AuthenticatedUser(
                id,
                TenantId.of(id),
                "joao@exemplo.com",
                "João",
                "Barbearia do João",
                "$2a$10$hash",
                "OWNER",
                true);
    }

    @Test
    @DisplayName("GET /cadastro entrega o formulário vazio")
    void formularioVazio() throws Exception {
        mockMvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    @DisplayName("cadastro válido redireciona para o painel")
    void cadastroValido() throws Exception {
        when(registerBusiness.register(any(RegisterBusinessCommand.class)))
                .thenReturn(registrado());
        when(userDetailsService.loadUserByUsername("joao@exemplo.com")).thenReturn(principal());

        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "joao@exemplo.com")
                        .param("password", "senha-do-joao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("a sessão é gravada no repositório, não só no holder — é o DD-5")
    void gravaContextoNaSessao() throws Exception {
        when(registerBusiness.register(any(RegisterBusinessCommand.class)))
                .thenReturn(registrado());
        when(userDetailsService.loadUserByUsername(any())).thenReturn(principal());

        mockMvc.perform(post("/cadastro")
                .with(csrf())
                .param("businessName", "Barbearia do João")
                .param("slug", "barbearia-do-joao")
                .param("email", "joao@exemplo.com")
                .param("password", "senha-do-joao"));

        // Sem esta chamada o usuário chegaria deslogado ao painel — e todo o
        // resto do teste continuaria verde.
        verify(securityContextRepository).saveContext(any(), any(), any());
    }

    @Test
    @DisplayName("sem token CSRF o POST é recusado")
    void exigeCsrf() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "joao@exemplo.com")
                        .param("password", "senha-do-joao"))
                .andExpect(status().isForbidden());

        verify(registerBusiness, never()).register(any());
    }

    @Test
    @DisplayName("erro de formato devolve a mesma tela com 200, não 400")
    void erroDeFormatoDevolveATela() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "X")
                        .param("slug", "AB")
                        .param("email", "nao-e-email")
                        .param("password", "curta"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"))
                .andExpect(model().attributeHasFieldErrors(
                        "form", "businessName", "slug", "email", "password"));

        verify(registerBusiness, never()).register(any());
    }

    @Test
    @DisplayName("o preenchimento é preservado quando há erro")
    void preservaOPreenchimento() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "nao-e-email")
                        .param("password", "senha-do-joao"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "email"))
                // hasProperty não serve para record: ele usa introspecção
                // JavaBeans e procuraria getBusinessName(), que não existe.
                .andExpect(result -> {
                    var form = (RegistrationRequest)
                            result.getModelAndView().getModel().get("form");
                    org.assertj.core.api.Assertions.assertThat(form.businessName())
                            .isEqualTo("Barbearia do João");
                    org.assertj.core.api.Assertions.assertThat(form.slug())
                            .isEqualTo("barbearia-do-joao");
                });
    }

    @Test
    @DisplayName("slug indisponível vira erro no campo do slug, não erro interno")
    void slugIndisponivelViraErroDeCampo() throws Exception {
        when(registerBusiness.register(any(RegisterBusinessCommand.class)))
                .thenThrow(new SlugUnavailableException("barbearia-do-joao"));

        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "joao@exemplo.com")
                        .param("password", "senha-do-joao"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"))
                .andExpect(model().attributeHasFieldErrors("form", "slug"));
    }

    @Test
    @DisplayName("e-mail já usado vira erro no campo do e-mail")
    void emailUsadoViraErroDeCampo() throws Exception {
        when(registerBusiness.register(any(RegisterBusinessCommand.class)))
                .thenThrow(new EmailAlreadyUsedException());

        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "joao@exemplo.com")
                        .param("password", "senha-do-joao"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "email"));
    }
}
