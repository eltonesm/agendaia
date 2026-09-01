package com.agendaia.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.organization.adapter.in.web.request.RegistrationRequest;
import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.organization.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * <strong>E2E-3</strong> da spec funcional: o link escolhido já está em uso.
 *
 * <p>Cenário 🔴 Critical, e o motivo é o AC-3: o cadastro grava
 * {@code Business} e {@code User} na mesma transação, então a recusa do segundo
 * não pode deixar metade gravada. Um estabelecimento sem dono é uma conta
 * inacessível — ninguém consegue entrar nela para consertar.
 *
 * <p>Postgres real via Testcontainers (AC-4): a garantia é a restrição de
 * unicidade do banco, e um banco em memória não a reproduziria fielmente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SlugUnavailableIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private com.agendaia.organization.domain.ProfessionalRepository professionalRepository;

    @BeforeEach
    void limpar() {
        // professional tem FK para business (TODO-002) — precisa sair primeiro.
        professionalRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();
    }

    private MockHttpServletRequestBuilder cadastro(String slug, String email) {
        return post("/cadastro")
                .with(csrf())
                .param("businessName", "Barbearia do João")
                .param("slug", slug)
                .param("email", email)
                .param("password", "senha-do-joao");
    }

    /** Deixa "barbearia-do-joao" ocupado. */
    private void primeiroCadastro() throws Exception {
        mockMvc.perform(cadastro("barbearia-do-joao", "joao@exemplo.com"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("E2E-3: segundo cadastro com o mesmo link é recusado no campo do link")
    void slugEmUsoViraErroDeCampo() throws Exception {
        primeiroCadastro();

        mockMvc.perform(cadastro("barbearia-do-joao", "maria@exemplo.com"))
                // Tela de cadastro com erro, nunca 500 e nunca redirecionamento.
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"))
                .andExpect(model().attributeHasFieldErrors("form", "slug"))
                // Recusa não autentica ninguém.
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("AC-3: nenhum Business ou User parcial sobra da tentativa recusada")
    void naoGravaNadaParcial() throws Exception {
        primeiroCadastro();

        mockMvc.perform(cadastro("barbearia-do-joao", "maria@exemplo.com"))
                .andExpect(status().isOk());

        assertThat(businessRepository.count()).as("só o primeiro estabelecimento").isEqualTo(1);
        assertThat(userRepository.count()).as("só o primeiro dono").isEqualTo(1);
        assertThat(userRepository.findByEmail("maria@exemplo.com")).isEmpty();
    }

    @Test
    @DisplayName("o erro sugere uma variação livre do link")
    void sugereVariacaoLivre() throws Exception {
        primeiroCadastro();

        mockMvc.perform(cadastro("barbearia-do-joao", "maria@exemplo.com"))
                .andExpect(result -> {
                    var erros = result.getModelAndView().getModelMap();
                    var binding = (org.springframework.validation.BindingResult)
                            erros.get("org.springframework.validation.BindingResult.form");
                    var mensagem = binding.getFieldError("slug").getDefaultMessage();
                    assertThat(mensagem).contains("barbearia-do-joao-2");
                });
    }

    @Test
    @DisplayName("nome, e-mail e link digitados permanecem no formulário")
    void preservaOPreenchimento() throws Exception {
        primeiroCadastro();

        mockMvc.perform(cadastro("barbearia-do-joao", "maria@exemplo.com"))
                // Record não tem getter JavaBeans, então hasProperty não serve.
                .andExpect(result -> {
                    var form = (RegistrationRequest)
                            result.getModelAndView().getModel().get("form");
                    assertThat(form.businessName()).isEqualTo("Barbearia do João");
                    assertThat(form.slug()).isEqualTo("barbearia-do-joao");
                    assertThat(form.email()).isEqualTo("maria@exemplo.com");
                });
    }

    @Test
    @DisplayName("palavra reservada é recusada do mesmo jeito, e sem sugerir variação")
    void palavraReservada() throws Exception {
        mockMvc.perform(cadastro("admin", "maria@exemplo.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"))
                .andExpect(model().attributeHasFieldErrors("form", "slug"))
                .andExpect(result -> {
                    var binding = (org.springframework.validation.BindingResult)
                            result.getModelAndView()
                                    .getModelMap()
                                    .get("org.springframework.validation.BindingResult.form");
                    // "admin-2" não é reservado, mas oferecê-lo ensinaria que o
                    // caminho está quase livre.
                    assertThat(binding.getFieldError("slug").getDefaultMessage())
                            .doesNotContain("admin-2");
                });

        assertThat(businessRepository.count()).isZero();
    }

    @Test
    @DisplayName("e-mail já usado é recusado no campo do e-mail, não no do link")
    void emailEmUso() throws Exception {
        primeiroCadastro();

        mockMvc.perform(cadastro("barbearia-do-joao-2", "joao@exemplo.com"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "email"))
                .andExpect(result -> {
                    var binding = (org.springframework.validation.BindingResult)
                            result.getModelAndView()
                                    .getModelMap()
                                    .get("org.springframework.validation.BindingResult.form");
                    assertThat(binding.getFieldError("slug"))
                            .as("o link estava livre; o erro é do e-mail")
                            .isNull();
                });
    }
}
