package com.agendaia.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.catalog.domain.ServiceOfferingRepository;
import com.agendaia.catalog.domain.ServiceRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.organization.domain.Professional;
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
 * E2E-1 e E2E-2 da spec funcional de cadastro-servico-oferta: caminho feliz
 * (serviço + oferta) e oferta duplicada recusada.
 *
 * <p>Postgres real via Testcontainers. Sem {@code @Transactional}, mesmo
 * motivo das ITs anteriores: em produção cada requisição abre a sua
 * transação, e a limpeza é feita à mão no {@code @BeforeEach}.
 *
 * <p>Ordem de limpeza: {@code service_offering} antes de {@code service}
 * (FK dentro do próprio contexto) e {@code professional} antes de
 * {@code business} (FK de {@code organization}) — mesma disciplina
 * documentada em {@code PATTERNS.md} para IT que compartilha o Postgres do
 * Testcontainers com as demais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ServiceOfferingRegistrationIT {

    private static final String EMAIL = "dono@exemplo.com";
    private static final String SENHA = "senha-do-dono";

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private ServiceOfferingRepository serviceOfferingRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Business barbearia;
    private Professional maria;

    @BeforeEach
    void semear() {
        serviceOfferingRepository.deleteAllInBatch();
        serviceRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();

        barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia do João", "barbearia-do-joao-ofertas"));
        userRepository.saveAndFlush(
                User.owner(barbearia.tenantId(), EMAIL, "João", passwordEncoder.encode(SENHA)));
        maria = professionalRepository.saveAndFlush(
                Professional.register(barbearia.tenantId(), "Maria Oliveira"));
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

    private java.util.UUID cadastrarServico(MockHttpSession sessao, String nome) throws Exception {
        mockMvc.perform(post("/admin/servicos").with(csrf()).session(sessao).param("name", nome))
                .andExpect(status().is3xxRedirection());
        return serviceRepository.findByTenantIdAndActiveTrueOrderByNameAsc(barbearia.id()).stream()
                .filter(servico -> servico.name().equals(nome))
                .findFirst()
                .orElseThrow()
                .id();
    }

    @Test
    @DisplayName("E2E-1: cadastro de servico e oferta, caminho feliz")
    void cadastroDeServicoEOferta() throws Exception {
        var sessao = sessaoAutenticada();
        var servicoId = cadastrarServico(sessao, "Corte de Cabelo");

        mockMvc.perform(post("/admin/ofertas")
                        .with(csrf())
                        .session(sessao)
                        .param("serviceId", servicoId.toString())
                        .param("professionalId", maria.id().toString())
                        .param("durationMinutes", "30")
                        .param("price", "30.00")
                        .param("bufferMinutes", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ofertas"));

        mockMvc.perform(get("/admin/ofertas").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Corte de Cabelo")))
                .andExpect(content().string(Matchers.containsString("Maria Oliveira")))
                .andExpect(content().string(Matchers.containsString("R$ 30,00")));

        var ofertas = serviceOfferingRepository.findByTenantIdAndActiveTrueOrderByCreatedAtAsc(barbearia.id());
        assertThat(ofertas).hasSize(1);
        assertThat(ofertas.getFirst().tenantId()).isEqualTo(barbearia.tenantId());
        assertThat(ofertas.getFirst().serviceId()).isEqualTo(servicoId);
        assertThat(ofertas.getFirst().professionalId()).isEqualTo(maria.id());
    }

    @Test
    @DisplayName("E2E-2: segunda oferta do mesmo (servico, profissional) e recusada, sem gravar")
    void ofertaDuplicadaERecusada() throws Exception {
        var sessao = sessaoAutenticada();
        var servicoId = cadastrarServico(sessao, "Corte de Cabelo");

        mockMvc.perform(post("/admin/ofertas")
                        .with(csrf())
                        .session(sessao)
                        .param("serviceId", servicoId.toString())
                        .param("professionalId", maria.id().toString())
                        .param("durationMinutes", "30")
                        .param("price", "30.00")
                        .param("bufferMinutes", "0"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/ofertas")
                        .with(csrf())
                        .session(sessao)
                        .param("serviceId", servicoId.toString())
                        .param("professionalId", maria.id().toString())
                        .param("durationMinutes", "45")
                        .param("price", "50.00")
                        .param("bufferMinutes", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "professionalId"));

        assertThat(serviceOfferingRepository.findByTenantIdAndActiveTrueOrderByCreatedAtAsc(barbearia.id()))
                .hasSize(1);
    }
}
