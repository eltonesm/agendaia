package com.agendaia.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.catalog.application.port.out.ServiceOfferingRepository;
import com.agendaia.catalog.application.port.out.ServiceRepository;
import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.UserRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.User;
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
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private ServiceOfferingRepository serviceOfferingRepository;
    @Autowired private com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository businessOperatingHoursRepository;
    @Autowired private com.agendaia.organization.application.port.out.WorkScheduleRepository workScheduleRepository;
    @Autowired private com.agendaia.organization.application.port.out.TimeOffRepository timeOffRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Business barbearia;
    private Business salao;

    @BeforeEach
    void semearDoisTenants() {
        serviceOfferingRepository.deleteAllInBatch();
        serviceRepository.deleteAllInBatch();
        timeOffRepository.deleteAllInBatch();
        workScheduleRepository.deleteAllInBatch();
        businessOperatingHoursRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
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
    @DisplayName("TODO-002: profissional cadastrado por um dono não aparece para o outro")
    void profissionalNaoAparecePeloOutroTenant() throws Exception {
        mockMvc.perform(post("/admin/profissionais")
                        .with(csrf())
                        .session(entrarComo("joao@exemplo.com"))
                        .param("name", "Pedro, funcionário do João"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/profissionais").session(entrarComo("maria@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(Matchers.not(Matchers.containsString("Pedro, funcionário do João"))));
    }

    @Test
    @DisplayName("TODO-002: o profissional cadastrado grava sempre no tenant de quem está logado")
    void profissionalGravaNoTenantDeQuemEstaLogado() throws Exception {
        mockMvc.perform(post("/admin/profissionais")
                        .with(csrf())
                        .session(entrarComo("joao@exemplo.com"))
                        .param("name", "Pedro"))
                .andExpect(status().is3xxRedirection());

        // Não há parâmetro de tenant para forjar (DD-1 da TODO-002) — o teste
        // confirma a ausência de vazamento, não a rejeição de um valor que
        // nunca existiu na assinatura.
        var profissionais =
                professionalRepository.findByTenantIdAndActiveTrueOrderByNameAsc(barbearia.id());
        assertThat(profissionais).hasSize(1);
        assertThat(profissionais.getFirst().tenantId()).isEqualTo(barbearia.tenantId());

        var profissionaisDaMaria =
                professionalRepository.findByTenantIdAndActiveTrueOrderByNameAsc(salao.id());
        assertThat(profissionaisDaMaria).isEmpty();
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

    @Test
    @DisplayName(
            "E2E-3 (cadastro-servico-oferta): oferta forjada com professionalId de outro tenant é recusada")
    void ofertaForjadaComProfissionalDeOutroTenantERecusada() throws Exception {
        var profissionalDaMaria =
                professionalRepository.saveAndFlush(Professional.register(salao.tenantId(), "Ana, funcionária da Maria"));

        mockMvc.perform(post("/admin/servicos")
                        .with(csrf())
                        .session(entrarComo("joao@exemplo.com"))
                        .param("name", "Corte de Cabelo"))
                .andExpect(status().is3xxRedirection());
        var servicoDoJoao = serviceRepository.findByTenantIdAndActiveTrueOrderByNameAsc(barbearia.id()).getFirst();

        // Requisição forjada: professionalId de um profissional que não aparece
        // no dropdown do tenant do João — não há chave estrangeira que impeça
        // isso no banco (DD-2 da spec técnica), a garantia é de aplicação.
        mockMvc.perform(post("/admin/ofertas")
                        .with(csrf())
                        .session(entrarComo("joao@exemplo.com"))
                        .param("serviceId", servicoDoJoao.id().toString())
                        .param("professionalId", profissionalDaMaria.id().toString())
                        .param("durationMinutes", "30")
                        .param("price", "30.00")
                        .param("bufferMinutes", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "professionalId"));

        assertThat(serviceOfferingRepository.findByTenantIdAndActiveTrueOrderByCreatedAtAsc(barbearia.id()))
                .isEmpty();
        assertThat(serviceOfferingRepository.count()).isZero();
    }

    @Test
    @DisplayName(
            "E2E-3 (horario-jornada-bloqueios): horario, jornada e bloqueio de um tenant nao aparecem para o outro")
    void horarioJornadaEBloqueioNaoAparecemParaOOutroTenant() throws Exception {
        var professionalDoJoao =
                professionalRepository.saveAndFlush(Professional.register(barbearia.tenantId(), "Pedro, funcionário do João"));

        var sessaoDoJoao = entrarComo("joao@exemplo.com");
        mockMvc.perform(post("/admin/horario-funcionamento")
                        .with(csrf())
                        .session(sessaoDoJoao)
                        .param("dayOfWeek", "MONDAY")
                        .param("opensAt", "08:00")
                        .param("closesAt", "18:00"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/jornadas")
                        .with(csrf())
                        .session(sessaoDoJoao)
                        .param("professionalId", professionalDoJoao.id().toString())
                        .param("dayOfWeek", "MONDAY")
                        .param("startsAt", "08:00")
                        .param("endsAt", "12:00"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/bloqueios")
                        .with(csrf())
                        .session(sessaoDoJoao)
                        .param("professionalId", "")
                        .param("startsAt", "2026-12-25T00:00")
                        .param("endsAt", "2026-12-26T00:00")
                        .param("reason", "Natal"))
                .andExpect(status().is3xxRedirection());

        var sessaoDaMaria = entrarComo("maria@exemplo.com");
        // Não verifica o texto do dia da semana aqui: o <select> do formulário
        // sempre lista os sete dias como opção estática, dado ou não —
        // "segunda-feira" apareceria mesmo com a lista vazia. A ausência de
        // dado é verificada direto no repositório, ao final do teste.
        mockMvc.perform(get("/admin/horario-funcionamento").session(sessaoDaMaria)).andExpect(status().isOk());
        mockMvc.perform(get("/admin/jornadas").session(sessaoDaMaria))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("Pedro, funcionário do João"))));
        mockMvc.perform(get("/admin/bloqueios").session(sessaoDaMaria))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("Natal"))));

        assertThat(businessOperatingHoursRepository.findByTenantIdAndActiveTrueOrderByDayOfWeekAscOpensAtAsc(
                        salao.id()))
                .isEmpty();
        assertThat(workScheduleRepository.findByTenantIdAndActiveTrueOrderByProfessionalIdAscDayOfWeekAscStartsAtAsc(
                        salao.id()))
                .isEmpty();
        assertThat(timeOffRepository.findByTenantIdAndActiveTrueOrderByStartsAtDesc(salao.id())).isEmpty();
    }
}
