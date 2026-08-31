package com.agendaia.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
 * <strong>E2E-2</strong> da spec funcional: entrar em conta existente, com
 * retorno à rota originalmente pretendida.
 *
 * <p>Postgres real via Testcontainers. O sufixo IT o coloca no Failsafe, que
 * roda no {@code verify} — o Surefire ignoraria este arquivo.
 *
 * <p>Onde é preciso reaproveitar a sessão entre requisições, o teste usa
 * {@code post("/login")} em vez de {@code formLogin()}: o construtor de
 * requisição do Spring Security não expõe {@code session()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginIT {

    private static final String EMAIL = "joao.login@exemplo.com";
    private static final String SENHA = "senha-do-joao";

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Business barbearia;

    @BeforeEach
    void semear() {
        // Sem @Transactional de propósito: em produção cada requisição abre a
        // sua transação, e uma transação ambiente do teste esconderia isso.
        // O preço é limpar à mão — o container é compartilhado entre os ITs.
        userRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();

        barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia do João", "barbearia-do-joao-login"));
        userRepository.saveAndFlush(
                User.owner(barbearia.tenantId(), EMAIL, "João", passwordEncoder.encode(SENHA)));
    }

    /** Login por POST comum, para poder carregar uma sessão existente. */
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder entrar(
            MockHttpSession sessao) {
        var requisicao = post("/login").with(csrf()).param("username", EMAIL).param("password", SENHA);
        return sessao == null ? requisicao : requisicao.session(sessao);
    }

    @Test
    @DisplayName("credencial correta autentica e leva ao painel")
    void loginValido() throws Exception {
        mockMvc.perform(formLogin("/login").user(EMAIL).password(SENHA))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("E2E-2: depois de entrar, volta para a rota que tentou abrir")
    void voltaParaDestinoPretendido() throws Exception {
        // A rota é /admin/agenda de propósito, e não o painel: o painel é o
        // defaultSuccessUrl, então usá-lo aqui faria o teste passar mesmo se o
        // destino pretendido fosse ignorado. Ela ainda não existe — chega numa
        // feature seguinte — mas isso não muda o que este cenário verifica, que
        // é para onde o Spring Security manda depois de autenticar.
        var sessao = (MockHttpSession) mockMvc.perform(get("/admin/agenda"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getRequest()
                .getSession();

        // Entra usando a MESMA sessão.
        //
        // O destino vem absoluto e com "?continue": desde o Spring Security 6.3
        // o request cache só devolve a requisição guardada quando esse parâmetro
        // está presente.
        mockMvc.perform(entrar(sessao))
                .andExpect(authenticated())
                .andExpect(header().string("Location", Matchers.endsWith("/admin/agenda?continue")))
                .andExpect(header().string("Location", Matchers.not(Matchers.containsString("dashboard"))));
    }

    @Test
    @DisplayName("senha errada não autentica e a mensagem é genérica")
    void senhaErrada() throws Exception {
        mockMvc.perform(formLogin("/login").user(EMAIL).password("errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    @DisplayName("e-mail inexistente produz o MESMO resultado que senha errada")
    void emailInexistente() throws Exception {
        mockMvc.perform(formLogin("/login").user("ninguem@exemplo.com").password(SENHA))
                .andExpect(unauthenticated())
                // Idêntico ao caso anterior: nada revela se o e-mail existe.
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    @DisplayName("estabelecimento desativado impede a entrada, com a mesma mensagem")
    void estabelecimentoDesativado() throws Exception {
        barbearia.deactivate();
        businessRepository.saveAndFlush(barbearia);

        mockMvc.perform(formLogin("/login").user(EMAIL).password(SENHA))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    @DisplayName("o painel mostra o link público do estabelecimento da sessão")
    void painelMostraOLink() throws Exception {
        var sessao = (MockHttpSession)
                mockMvc.perform(entrar(null)).andReturn().getRequest().getSession();

        mockMvc.perform(get("/admin/dashboard").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("barbearia-do-joao-login")))
                .andExpect(content().string(Matchers.containsString("Barbearia do João")));
    }

    @Test
    @DisplayName("sair invalida a sessão no servidor, não só no navegador")
    void logoutInvalidaSessao() throws Exception {
        var sessao = (MockHttpSession)
                mockMvc.perform(entrar(null)).andReturn().getRequest().getSession();

        mockMvc.perform(post("/logout").with(csrf()).session(sessao))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?saiu"));

        assertThat(sessao.isInvalid())
                .as("voltar pelo histórico não pode devolver acesso")
                .isTrue();
    }

    @Test
    @DisplayName("sem sessão, o painel manda para o login")
    void painelExigeSessao() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", Matchers.containsString("/login")));
    }
}
