package com.agendaia.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.billing.application.port.out.BillingAccountRepository;
import com.agendaia.billing.domain.BillingAccount;
import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.application.port.out.UserRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.User;
import java.time.LocalDate;
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
 * Ponta a ponta do back-office do operador (TODO-009), Postgres real via
 * Testcontainers, cobrindo E2E-1 a E2E-5 da spec funcional.
 *
 * <p>Credencial do operador vem do padrão de desenvolvimento em
 * {@code application.yaml} ({@code operador}/{@code operador-dev-only}) —
 * nenhuma variável de ambiente sobrescreve isso nos testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OperadorBackofficeIT {

    private static final String SENHA_DONO = "senha-de-teste";
    private static final String OPERADOR_USERNAME = "operador";
    private static final String OPERADOR_SENHA = "operador-dev-only";

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BillingAccountRepository billingAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limpar() {
        billingAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();
    }

    private Business criarEstabelecimento(String nome, String slug, String email) {
        var business = businessRepository.saveAndFlush(Business.register(nome, slug));
        userRepository.saveAndFlush(
                User.owner(business.tenantId(), email, nome, passwordEncoder.encode(SENHA_DONO)));
        return business;
    }

    /** Seed direto: contorna o get-or-create para simular um trial já vencido. */
    private BillingAccount seedContaComCadastroEm(Business business, LocalDate registeredOn) {
        var conta = BillingAccount.startTrial(business.tenantId().value(), registeredOn);
        return billingAccountRepository.saveAndFlush(conta);
    }

    private MockHttpSession entrarComoDono(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", SENHA_DONO))
                .andReturn()
                .getRequest()
                .getSession();
    }

    private MockHttpSession entrarComoOperador() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/operador/login")
                        .with(csrf())
                        .param("username", OPERADOR_USERNAME)
                        .param("password", OPERADOR_SENHA))
                .andReturn()
                .getRequest()
                .getSession();
    }

    @Test
    @DisplayName("E2E-1: cadastro gera trial e aparece no painel do operador")
    void e2e1CadastroGeraTrialEApareceNoPainel() throws Exception {
        var business = criarEstabelecimento("Barbearia Nova", "barbearia-nova", "nova@exemplo.com");
        var sessaoOperador = entrarComoOperador();

        mockMvc.perform(get("/operador/painel").session(sessaoOperador))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Barbearia Nova")))
                .andExpect(content().string(Matchers.containsString("Em teste")));

        var conta = billingAccountRepository.findByTenantId(business.tenantId().value()).orElseThrow();
        assertThat(conta.trialEndsAt()).isEqualTo(conta.accessValidUntil());
        assertThat(conta.accessValidUntil()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    @DisplayName("E2E-2: carência mostra aviso, sem bloquear")
    void e2e2CarenciaMostraAvisoSemBloquear() throws Exception {
        var business = criarEstabelecimento("Salão em Carência", "salao-em-carencia", "carencia@exemplo.com");
        // registeredOn = hoje - 33 dias -> accessValidUntil = hoje - 3 dias,
        // dentro dos 5 dias corridos de carência (BR-4).
        seedContaComCadastroEm(business, LocalDate.now().minusDays(33));

        var sessaoDono = entrarComoDono("carencia@exemplo.com");

        mockMvc.perform(get("/admin/dashboard").session(sessaoDono))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("período gratuito acabou")));
    }

    @Test
    @DisplayName("E2E-3: bloqueio automático após a carência")
    void e2e3BloqueioAutomaticoAposACarencia() throws Exception {
        var business = criarEstabelecimento("Salão Bloqueado", "salao-bloqueado", "bloqueado@exemplo.com");
        // registeredOn = hoje - 40 dias -> accessValidUntil = hoje - 10 dias,
        // mais de 5 dias além da carência (BR-4).
        seedContaComCadastroEm(business, LocalDate.now().minusDays(40));

        var sessaoDono = entrarComoDono("bloqueado@exemplo.com");

        mockMvc.perform(get("/admin/dashboard").session(sessaoDono))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/conta-suspensa"));

        mockMvc.perform(get("/admin/conta-suspensa").session(sessaoDono))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("suspensa")));

        var sessaoOperador = entrarComoOperador();
        mockMvc.perform(get("/operador/painel").session(sessaoOperador))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Bloqueado")));
    }

    @Test
    @DisplayName("E2E-4: marcar como pago libera o acesso")
    void e2e4MarcarComoPagoLiberaOAcesso() throws Exception {
        var business = criarEstabelecimento("Salão a Regularizar", "salao-a-regularizar", "regularizar@exemplo.com");
        seedContaComCadastroEm(business, LocalDate.now().minusDays(40));

        var sessaoDono = entrarComoDono("regularizar@exemplo.com");
        mockMvc.perform(get("/admin/dashboard").session(sessaoDono))
                .andExpect(status().is3xxRedirection());

        var sessaoOperador = entrarComoOperador();
        var novaData = LocalDate.now().plusDays(30);
        mockMvc.perform(post(
                                "/operador/estabelecimentos/{tenantId}/prazo",
                                business.tenantId().value())
                        .session(sessaoOperador)
                        .with(csrf())
                        .param("accessValidUntil", novaData.toString()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/dashboard").session(sessaoDono))
                .andExpect(status().isOk());

        mockMvc.perform(get("/operador/painel").session(sessaoOperador))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Pago")));
    }

    @Test
    @DisplayName("E2E-5: isolamento entre login de operador e login de dono")
    void e2e5IsolamentoEntreLoginDeOperadorEDeDono() throws Exception {
        criarEstabelecimento("Estabelecimento Qualquer", "estabelecimento-qualquer", "dono@exemplo.com");

        // As duas cadeias de segurança compartilham o mesmo
        // SecurityContextRepository — sem a authority certa (hasRole, não só
        // authenticated()), a sessão de um passaria pela cadeia do outro
        // (BR-6). Já autenticada mas sem o papel exigido, a resposta é 403
        // (AccessDeniedHandler), não um redirecionamento para o login.
        var sessaoDono = entrarComoDono("dono@exemplo.com");
        mockMvc.perform(get("/operador/painel").session(sessaoDono)).andExpect(status().isForbidden());

        var sessaoOperador = entrarComoOperador();
        mockMvc.perform(get("/admin/dashboard").session(sessaoOperador)).andExpect(status().isForbidden());
    }
}
