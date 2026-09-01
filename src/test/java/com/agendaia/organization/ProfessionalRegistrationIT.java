package com.agendaia.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.organization.domain.ProfessionalRepository;
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
 * E2E-1, E2E-2 e E2E-3 da spec funcional da TODO-002: cadastro do primeiro
 * profissional, nome inválido, e múltiplos profissionais com nome repetido.
 *
 * <p>Postgres real via Testcontainers (AC-4). Sem {@code @Transactional}, mesmo
 * motivo do {@code RegistrationIT} da TODO-001: em produção cada requisição
 * abre a sua transação, e a limpeza é feita à mão no {@code @BeforeEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProfessionalRegistrationIT {

    private static final String EMAIL = "dono@exemplo.com";
    private static final String SENHA = "senha-do-dono";

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Business barbearia;

    @BeforeEach
    void semear() throws Exception {
        professionalRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();

        barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia do João", "barbearia-do-joao-profissionais"));
        userRepository.saveAndFlush(
                User.owner(barbearia.tenantId(), EMAIL, "João", passwordEncoder.encode(SENHA)));
    }

    private MockHttpSession sessaoAutenticada() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", EMAIL)
                        .param("password", SENHA))
                .andReturn()
                .getRequest()
                .getSession();
    }

    @Test
    @DisplayName("E2E-1: cadastro do primeiro profissional aparece na lista, com o tenant da sessao")
    void cadastroDoPrimeiroProfissional() throws Exception {
        var sessao = sessaoAutenticada();

        mockMvc.perform(post("/admin/profissionais")
                        .with(csrf())
                        .session(sessao)
                        .param("name", "Maria Oliveira"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/profissionais"));

        mockMvc.perform(get("/admin/profissionais").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Maria Oliveira")));

        var profissionais =
                professionalRepository.findByTenantIdAndActiveTrueOrderByNameAsc(barbearia.id());
        assertThat(profissionais).hasSize(1);
        assertThat(profissionais.getFirst().tenantId()).isEqualTo(barbearia.tenantId());
    }

    @Test
    @DisplayName("E2E-2: nome vazio e recusado sem gravar, nunca 500")
    void nomeVazioNaoGrava() throws Exception {
        var sessao = sessaoAutenticada();

        mockMvc.perform(post("/admin/profissionais").with(csrf()).session(sessao).param("name", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "name"));

        assertThat(professionalRepository.count()).isZero();
    }

    @Test
    @DisplayName("E2E-3: dois profissionais com o mesmo nome coexistem, cada um com seu id")
    void multiplosProfissionaisComNomeRepetido() throws Exception {
        var sessao = sessaoAutenticada();

        mockMvc.perform(post("/admin/profissionais")
                        .with(csrf())
                        .session(sessao)
                        .param("name", "Ana Souza"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/profissionais")
                        .with(csrf())
                        .session(sessao)
                        .param("name", "Ana Souza"))
                .andExpect(status().is3xxRedirection());

        var profissionais =
                professionalRepository.findByTenantIdAndActiveTrueOrderByNameAsc(barbearia.id());
        assertThat(profissionais).hasSize(2);
        assertThat(profissionais.get(0).id()).isNotEqualTo(profissionais.get(1).id());
        assertThat(profissionais).allMatch(p -> p.name().equals("Ana Souza"));
    }

    @Test
    @DisplayName("estabelecimento sem profissional mostra a lista vazia, com chamada para cadastrar")
    void semProfissionalMostraChamada() throws Exception {
        var sessao = sessaoAutenticada();

        mockMvc.perform(get("/admin/profissionais").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Nenhum profissional")));
    }

    @Test
    @DisplayName("sem sessao, a rota manda para o login")
    void semSessaoExigeLogin() throws Exception {
        mockMvc.perform(get("/admin/profissionais"))
                .andExpect(status().is3xxRedirection());
    }
}
