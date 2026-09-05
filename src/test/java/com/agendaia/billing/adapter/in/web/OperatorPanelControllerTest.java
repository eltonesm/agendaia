package com.agendaia.billing.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.agendaia.billing.application.BillingAccountService;
import com.agendaia.billing.application.EstablishmentView;
import com.agendaia.billing.domain.AccessStatus;
import com.agendaia.platform.security.OperatorSecurityConfig;
import com.agendaia.platform.security.SecurityConfig;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Camada web isolada: BillingAccountService é mock, o banco não existe. */
@WebMvcTest(OperatorPanelController.class)
@Import({OperatorSecurityConfig.class, SecurityConfig.class})
@WithMockUser(roles = "OPERATOR")
class OperatorPanelControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private BillingAccountService billingAccountService;

    private EstablishmentView estabelecimento(UUID tenantId, AccessStatus status) {
        return new EstablishmentView(
                tenantId, "Barbearia do João", "barbearia-do-joao", Instant.now(), status, LocalDate.now().plusDays(30));
    }

    @Test
    @DisplayName("GET /operador/painel lista os estabelecimentos com o status calculado")
    void listaEstabelecimentos() throws Exception {
        var tenantId = UuidV7.generate();
        when(billingAccountService.listForOperator())
                .thenReturn(List.of(estabelecimento(tenantId, AccessStatus.TRIAL)));

        mockMvc.perform(get("/operador/painel"))
                .andExpect(status().isOk())
                .andExpect(view().name("operador/painel"))
                .andExpect(model().attributeExists("estabelecimentos"));
    }

    @Test
    @DisplayName("lista vazia não é erro — só nenhum estabelecimento cadastrado ainda")
    void listaVaziaNaoEErro() throws Exception {
        when(billingAccountService.listForOperator()).thenReturn(List.of());

        mockMvc.perform(get("/operador/painel")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("marcar prazo com data futura redireciona (PRG) e delega ao serviço")
    void marcarPrazoValido() throws Exception {
        var tenantId = UuidV7.generate();
        var novaData = LocalDate.now().plusDays(30);

        mockMvc.perform(post("/operador/estabelecimentos/{tenantId}/prazo", tenantId)
                        .with(csrf())
                        .param("accessValidUntil", novaData.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/operador/painel"));

        verify(billingAccountService).extendUntil(eq(tenantId), eq(novaData));
    }

    @Test
    @DisplayName("marcar prazo com data no passado devolve a mesma tela, sem chamar o serviço")
    void marcarPrazoComDataNoPassadoViraErroDeCampo() throws Exception {
        var tenantId = UuidV7.generate();
        when(billingAccountService.listForOperator()).thenReturn(List.of());

        mockMvc.perform(post("/operador/estabelecimentos/{tenantId}/prazo", tenantId)
                        .with(csrf())
                        .param("accessValidUntil", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("operador/painel"))
                .andExpect(model().attribute("erroTenantId", tenantId));

        verify(billingAccountService, never()).extendUntil(any(), any());
    }

    @Test
    @DisplayName("sem token CSRF, o POST de marcar prazo é recusado (AC-1)")
    void marcarPrazoSemCsrfERecusado() throws Exception {
        var tenantId = UuidV7.generate();

        mockMvc.perform(post("/operador/estabelecimentos/{tenantId}/prazo", tenantId)
                        .param("accessValidUntil", LocalDate.now().plusDays(30).toString()))
                .andExpect(status().isForbidden());

        verify(billingAccountService, never()).extendUntil(any(), any());
    }

    @Test
    @DisplayName("sem sessão, a rota do operador exige login")
    void semSessaoExigeLogin() throws Exception {
        mockMvc.perform(get("/operador/painel").with(anonymous())).andExpect(status().is3xxRedirection());
    }
}
