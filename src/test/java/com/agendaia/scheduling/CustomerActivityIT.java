package com.agendaia.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.customer.application.port.out.CustomerRepository;
import com.agendaia.customer.domain.Customer;
import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.application.port.out.UserRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.User;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.PaymentStatus;
import com.agendaia.shared.Money;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
 * E2E-1 a E2E-4 e E2E-6 da spec funcional de gestao-de-clientes, ponta a
 * ponta contra Postgres real via Testcontainers. E2E-5 (marcar fiado
 * reflete no total em aberto) mora em {@link AgendaProfissionalIT}, mais
 * perto da rota de origem (agenda).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CustomerActivityIT {

    private static final String SENHA = "senha-do-dono";

    @Autowired private MockMvc mockMvc;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private record Cenario(Business barbearia, String email) {}

    private Cenario semearEstabelecimento() {
        var sufixo = UuidV7.generate().toString();
        var email = "dono-" + sufixo + "@exemplo.com";
        var barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia Clientes " + sufixo, "barbearia-clientes-" + sufixo));
        userRepository.saveAndFlush(
                User.owner(barbearia.tenantId(), email, "Dono " + sufixo, passwordEncoder.encode(SENHA)));
        return new Cenario(barbearia, email);
    }

    private MockHttpSession sessaoAutenticada(Cenario cenario) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", cenario.email())
                        .param("password", SENHA))
                .andReturn()
                .getRequest()
                .getSession();
    }

    private UUID semearCliente(Cenario cenario, String nome, String telefone) {
        return customerRepository
                .saveAndFlush(Customer.register(cenario.barbearia().tenantId(), nome, telefone))
                .id();
    }

    private void semearAtendimentoCompleto(
            Cenario cenario, UUID customerId, Money preco, PaymentStatus paymentStatus, Instant startsAt) {
        var agendamento = Appointment.reconstitute(
                UuidV7.generate(),
                cenario.barbearia().tenantId(),
                UuidV7.generate(),
                UuidV7.generate(),
                customerId,
                AppointmentStatus.COMPLETED,
                startsAt,
                startsAt.plus(30, ChronoUnit.MINUTES),
                "Corte de Cabelo",
                30,
                preco,
                paymentStatus);
        appointmentRepository.save(agendamento);
    }

    @Test
    @DisplayName("E2E-1: lista de clientes pagina corretamente com mais clientes do que cabem numa pagina")
    void e2e1ListaPaginaCorretamente() throws Exception {
        var cenario = semearEstabelecimento();
        var sessao = sessaoAutenticada(cenario);
        for (var i = 0; i < 22; i++) {
            semearCliente(cenario, "Cliente %02d".formatted(i), "119888800%02d".formatted(i));
        }

        mockMvc.perform(get("/admin/clientes").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cliente 00")));

        mockMvc.perform(get("/admin/clientes").session(sessao).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cliente 20")));
    }

    @Test
    @DisplayName("E2E-2: cliente sem nenhum atendimento COMPLETED aparece sinalizado como novo (BR-1)")
    void e2e2ClienteSemHistoricoENovo() throws Exception {
        var cenario = semearEstabelecimento();
        var sessao = sessaoAutenticada(cenario);
        semearCliente(cenario, "Cliente Novo", "11988889000");

        mockMvc.perform(get("/admin/clientes").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Novo")));
    }

    @Test
    @DisplayName(
            "E2E-3: detalhe do cliente mostra historico e totais corretos, cancelado nao entra em nenhuma conta")
    void e2e3DetalheComHistoricoETotais() throws Exception {
        var cenario = semearEstabelecimento();
        var sessao = sessaoAutenticada(cenario);
        var clienteId = semearCliente(cenario, "Cliente Detalhe", "11988889001");
        var agora = Instant.now();
        semearAtendimentoCompleto(cenario, clienteId, new Money(5000), PaymentStatus.PAID, agora.minus(2, ChronoUnit.DAYS));
        semearAtendimentoCompleto(
                cenario, clienteId, new Money(3000), PaymentStatus.ON_CREDIT, agora.minus(1, ChronoUnit.DAYS));
        // Cancelado nao conta em nenhum total (BR-1/BR-5) - semeado direto, sem passar pela agenda.
        var cancelado = Appointment.reconstitute(
                UuidV7.generate(),
                cenario.barbearia().tenantId(),
                UuidV7.generate(),
                UuidV7.generate(),
                clienteId,
                AppointmentStatus.CANCELLED,
                agora.minus(3, ChronoUnit.DAYS),
                agora.minus(3, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
                "Corte de Cabelo",
                30,
                new Money(9999),
                PaymentStatus.PENDING);
        appointmentRepository.save(cancelado);

        mockMvc.perform(get("/admin/clientes/{id}", clienteId).session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("R$ 80,00"))) // total gasto: 5000 + 3000
                .andExpect(content().string(containsString("R$ 30,00"))) // total em aberto: só o ON_CREDIT
                .andExpect(content().string(containsString("Fiado")))
                .andExpect(content().string(containsString("Pago")));
    }

    @Test
    @DisplayName("E2E-4: link de WhatsApp usa o telefone do cliente certo")
    void e2e4LinkWhatsappComTelefoneCerto() throws Exception {
        var cenario = semearEstabelecimento();
        var sessao = sessaoAutenticada(cenario);
        semearCliente(cenario, "Cliente WhatsApp", "11988889002");

        mockMvc.perform(get("/admin/clientes").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wa.me/11988889002")));
    }

    @Test
    @DisplayName("E2E-6: cliente de outro tenant nunca aparece na lista nem no detalhe")
    void e2e6IsolamentoEntreTenants() throws Exception {
        var cenarioA = semearEstabelecimento();
        var cenarioB = semearEstabelecimento();
        var sessaoA = sessaoAutenticada(cenarioA);
        var clienteDoB = semearCliente(cenarioB, "Cliente Isolado B", "11988889003");

        mockMvc.perform(get("/admin/clientes").session(sessaoA))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Cliente Isolado B"))));

        mockMvc.perform(get("/admin/clientes/{id}", clienteDoB).session(sessaoA)).andExpect(status().isNotFound());
    }
}
