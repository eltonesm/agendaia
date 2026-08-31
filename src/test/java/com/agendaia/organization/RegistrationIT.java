package com.agendaia.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.organization.domain.UserRepository;
import com.agendaia.platform.security.AuthenticatedUser;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

/**
 * <strong>E2E-1</strong> da spec funcional: cadastro completo até o painel.
 *
 * <p>O cenário é 🔴 Critical e existe por um motivo só: <strong>seguir o
 * redirecionamento</strong>. Um teste que parasse no {@code 302} passaria
 * mesmo se o DD-5 estivesse errado — o {@code SecurityContextHolder} vive na
 * thread do POST e some depois dele. Só a requisição seguinte, carregando a
 * sessão, prova que a autenticação sobreviveu.
 *
 * <p>Postgres real via Testcontainers (AC-4). Sem {@code @Transactional}: em
 * produção cada requisição abre a sua transação, e uma transação ambiente do
 * teste esconderia o fato de o cadastro ter que comitar antes do redirect.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void limpar() {
        userRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("E2E-1: cadastro leva ao painel JÁ AUTENTICADO, sem passar pelo login")
    void cadastroAteOPainel() throws Exception {
        // 1. O formulário é público — é a porta de entrada do produto.
        mockMvc.perform(get("/cadastro")).andExpect(status().isOk());

        // 2. Confirma o cadastro.
        var sessao = (MockHttpSession) mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "joao@exemplo.com")
                        .param("password", "senha-do-joao"))
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andReturn()
                .getRequest()
                .getSession();

        // 3. AC-1: segue o redirecionamento com a MESMA sessão. Aqui é onde o
        // DD-5 vive ou morre — sem o saveContext no repositório, esta
        // requisição cairia no /login.
        mockMvc.perform(get("/admin/dashboard").session(sessao))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("joao@exemplo.com"))
                .andExpect(content().string(Matchers.containsString("Barbearia do João")))
                .andExpect(content().string(Matchers.containsString("barbearia-do-joao")));
    }

    @Test
    @DisplayName("gravou um Business e um User, ativos, com o mesmo tenant")
    void gravouOsDoisComOMesmoTenant() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "joao@exemplo.com")
                        .param("password", "senha-do-joao"))
                .andExpect(redirectedUrl("/admin/dashboard"));

        var negocios = businessRepository.findAll();
        var usuarios = userRepository.findAll();

        assertThat(negocios).hasSize(1);
        assertThat(usuarios).hasSize(1);

        var business = negocios.getFirst();
        var user = usuarios.getFirst();

        assertThat(business.isActive()).isTrue();
        assertThat(user.isActive()).isTrue();
        // O tenant É o estabelecimento (ADR 0003): o dono nasce dentro dele.
        assertThat(user.tenantId()).isEqualTo(business.tenantId());
        assertThat(user.email()).isEqualTo("joao@exemplo.com");
    }

    @Test
    @DisplayName("a senha vai para o banco em hash, nunca em texto")
    void senhaNuncaEmTexto() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "joao@exemplo.com")
                        .param("password", "senha-do-joao"))
                .andExpect(redirectedUrl("/admin/dashboard"));

        var hash = userRepository.findAll().getFirst().passwordHash();

        assertThat(hash).doesNotContain("senha-do-joao").startsWith("$2");
    }

    @Test
    @DisplayName("e-mail é normalizado para minúsculas antes de gravar")
    void emailNormalizado() throws Exception {
        // Sem espaço nas pontas de propósito: o @Email do Bean Validation roda
        // antes do strip() do caso de uso e recusaria " joao@... ". Não é
        // problema no produto — o input type=email já entrega o valor aparado
        // pelo navegador — mas seria um teste testando o que o servidor não faz.
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "JOAO@Exemplo.COM")
                        .param("password", "senha-do-joao"))
                .andExpect(redirectedUrl("/admin/dashboard"));

        assertThat(userRepository.findAll().getFirst().email()).isEqualTo("joao@exemplo.com");
    }

    @Test
    @DisplayName("o hash da senha não fica guardado na sessão")
    void hashNaoFicaNaSessao() throws Exception {
        var sessao = (MockHttpSession) mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "joao@exemplo.com")
                        .param("password", "senha-do-joao"))
                .andReturn()
                .getRequest()
                .getSession();

        var contexto = (SecurityContext)
                sessao.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        var principal = (AuthenticatedUser) contexto.getAuthentication().getPrincipal();

        // O cadastro autentica à mão, sem passar por provider: se o controller
        // esquecer o eraseCredentials, o hash BCrypt vai para a sessão e a
        // acompanha até onde ela for guardada.
        assertThat(principal.getPassword()).isNull();
        assertThat(principal.tenantId()).isNotNull();
    }

    @Test
    @DisplayName("POST sem token CSRF é recusado e não grava nada")
    void semCsrfNaoGrava() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .param("businessName", "Barbearia do João")
                        .param("slug", "barbearia-do-joao")
                        .param("email", "joao@exemplo.com")
                        .param("password", "senha-do-joao"))
                .andExpect(status().isForbidden());

        assertThat(businessRepository.count()).isZero();
        assertThat(userRepository.count()).isZero();
    }
}
