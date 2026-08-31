package com.agendaia.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.organization.domain.User;
import com.agendaia.organization.domain.UserRepository;
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
 * Isolamento entre estabelecimentos.
 *
 * <p>Não é o teste de uma feature: é o teste da premissa do produto. Vaza uma
 * vez e o produto acabou — um dono vendo a agenda de outro não é defeito que se
 * conserta com um patch e um pedido de desculpas.
 *
 * <p><strong>Este arquivo cresce a cada feature.</strong> Hoje só existe uma
 * rota autenticada, então há pouco a verificar; a cada rota nova, um caso novo
 * entra aqui. Uma rota sem caso correspondente é uma rota não verificada.
 *
 * <p>Vive em {@code platform} de propósito, e não em {@code organization}: o
 * que ele protege é o mecanismo de tenant, que é da plataforma. O dia em que
 * {@code scheduling} tiver rota, o caso dela entra neste mesmo arquivo.
 *
 * <h2>Verificação do AC-3</h2>
 *
 * <p>Um teste de isolamento que passa por acidente é pior que nenhum. Este foi
 * conferido desligando o mecanismo de propósito: com o
 * {@code TenantContextFilter} declarando {@code @Order(Integer.MIN_VALUE + 100)}
 * — ou seja, rodando <em>antes</em> da cadeia do Spring Security, onde o
 * principal ainda não existe — três dos cinco casos falham. A ordem foi
 * restaurada em seguida.
 *
 * <p>Vale registrar <em>como</em> falham: com <strong>500</strong>, não com
 * dado do outro tenant na tela. O {@code TenantContext.require()} recusa em vez
 * de devolver algo, então o mecanismo falha fechado. É a propriedade que se
 * quer, e é o motivo de a ausência de tenant ser exceção e não
 * {@code Optional} vazio: o segundo faria a rota devolver 200 com nada, e um
 * teste de conteúdo vazio passa por acidente com facilidade.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CrossTenantIsolationIT {

    private static final String SENHA = "senha-de-teste";

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Business barbearia;
    private Business salao;

    @BeforeEach
    void semearDoisTenants() {
        userRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();

        barbearia = criar("Barbearia do João", "barbearia-do-joao", "joao@exemplo.com");
        salao = criar("Salão da Maria", "salao-da-maria", "maria@exemplo.com");
    }

    private Business criar(String nome, String slug, String email) {
        var business = businessRepository.saveAndFlush(Business.register(nome, slug));
        userRepository.saveAndFlush(
                User.owner(business.tenantId(), email, nome, passwordEncoder.encode(SENHA)));
        return business;
    }

    private MockHttpSession entrarComo(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", SENHA))
                .andReturn()
                .getRequest()
                .getSession();
    }

    @Test
    @DisplayName("AC-1/AC-2: cada dono vê apenas o seu estabelecimento no painel")
    void cadaDonoVeApenasOSeuEstabelecimento() throws Exception {
        mockMvc.perform(get("/admin/dashboard").session(entrarComo("joao@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Barbearia do João")))
                .andExpect(content().string(Matchers.containsString("barbearia-do-joao")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Salão da Maria"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("salao-da-maria"))));

        mockMvc.perform(get("/admin/dashboard").session(entrarComo("maria@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Salão da Maria")))
                .andExpect(content().string(Matchers.containsString("salao-da-maria")))
                .andExpect(content()
                        .string(Matchers.not(Matchers.containsString("Barbearia do João"))))
                .andExpect(content()
                        .string(Matchers.not(Matchers.containsString("barbearia-do-joao"))));
    }

    @Test
    @DisplayName("AC-2: tenant vem da sessão, e parâmetro do cliente não muda nada")
    void tenantVemDaSessaoNuncaDoCliente() throws Exception {
        var sessaoDoJoao = entrarComo("joao@exemplo.com");

        // Tentativa explícita de trocar de tenant pela URL. A regra do CLAUDE.md
        // é que tenantId nunca vem do cliente; aqui ela é exercida, não só dita.
        mockMvc.perform(get("/admin/dashboard")
                        .session(sessaoDoJoao)
                        .param("tenantId", salao.tenantId().value().toString())
                        .param("businessId", salao.id().toString())
                        .param("slug", "salao-da-maria"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Barbearia do João")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Salão da Maria"))));
    }

    @Test
    @DisplayName("AC-2: cabeçalho forjado também não troca o tenant")
    void cabecalhoForjadoNaoTrocaOTenant() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .session(entrarComo("joao@exemplo.com"))
                        .header("X-Tenant-Id", salao.tenantId().value().toString())
                        .header("X-Business-Slug", "salao-da-maria"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Barbearia do João")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Salão da Maria"))));
    }

    @Test
    @DisplayName("sair de um tenant não deixa a sessão utilizável pelo outro")
    void sairInvalidaAcesso() throws Exception {
        var sessaoDoJoao = entrarComo("joao@exemplo.com");

        mockMvc.perform(post("/logout").with(csrf()).session(sessaoDoJoao))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/dashboard").session(sessaoDoJoao))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("os dois tenants existem e são distinguíveis no banco")
    void osDoisTenantsSaoDistintos() {
        assertThat(barbearia.tenantId()).isNotEqualTo(salao.tenantId());
        assertThat(businessRepository.count()).isEqualTo(2);
        assertThat(userRepository.count()).isEqualTo(2);

        var joao = userRepository.findByEmail("joao@exemplo.com").orElseThrow();
        var maria = userRepository.findByEmail("maria@exemplo.com").orElseThrow();

        assertThat(joao.tenantId()).isEqualTo(barbearia.tenantId());
        assertThat(maria.tenantId()).isEqualTo(salao.tenantId());
        assertThat(joao.tenantId()).isNotEqualTo(maria.tenantId());
    }
}
