package com.agendaia.scheduling.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.agendaia.catalog.api.PublicOfferingRef;
import com.agendaia.catalog.api.PublicServiceRef;
import com.agendaia.catalog.api.ServiceDirectory;
import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.organization.api.BusinessDirectory;
import com.agendaia.organization.api.BusinessRef;
import com.agendaia.platform.security.SecurityConfig;
import com.agendaia.scheduling.application.port.in.BookAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.BookedAppointment;
import com.agendaia.scheduling.application.port.in.GetAvailableSlotsUseCase;
import com.agendaia.scheduling.domain.AvailableSlot;
import com.agendaia.scheduling.domain.exception.SlotUnavailableException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Camada web isolada: os casos de uso são mock, o banco não existe.
 *
 * <p>{@code BusinessDirectory} mockado para o {@code TenantContextFilter}
 * real (entra nesta fatia por ser {@code @Component} global) resolver o
 * tenant pelo slug de verdade, exatamente como em produção — sem isso,
 * toda rota devolveria 404 por falta de tenant no contexto.
 *
 * <p>{@code BookingRateLimiter} é importado de verdade (não mock) para
 * testar o rate limit real (BR-8) — mas como é um {@code @Bean} singleton
 * e o contexto Spring é cacheado entre os métodos desta classe, cada
 * teste que faz {@code POST} usa um IP simulado próprio ({@link #ipUnico}),
 * para não compartilhar o "balde" de tentativas com os demais.
 */
@WebMvcTest(PublicBookingController.class)
@Import({SecurityConfig.class, BookingRateLimiter.class})
class PublicBookingControllerTest {

    private static final String SLUG = "barbearia-teste";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ServiceDirectory serviceDirectory;
    @MockitoBean private ServiceOfferingDirectory serviceOfferingDirectory;
    @MockitoBean private GetAvailableSlotsUseCase getAvailableSlots;
    @MockitoBean private BookAppointmentUseCase bookAppointment;
    @MockitoBean private BusinessDirectory businessDirectory;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID offeringId = UUID.randomUUID();

    private static int proximoIp = 1;

    @BeforeEach
    void resolverTenantPeloSlug() {
        when(businessDirectory.findBySlug(SLUG))
                .thenReturn(Optional.of(new BusinessRef(tenantId, "Barbearia Teste", SLUG, Instant.now())));
    }

    /** IP simulado exclusivo deste método de teste — isola o BookingRateLimiter entre testes. */
    private static RequestPostProcessor ipUnico() {
        var ip = "10.0.0." + (proximoIp++);
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    @DisplayName("GET /b/{slug} lista os serviços ativos")
    void catalogoListaServicos() throws Exception {
        var serviceId = UUID.randomUUID();
        when(serviceDirectory.listActive()).thenReturn(List.of(new PublicServiceRef(serviceId, "Corte de Cabelo")));

        mockMvc.perform(get("/b/{slug}", SLUG))
                .andExpect(status().isOk())
                .andExpect(view().name("public/catalogo"))
                .andExpect(content().string(Matchers.containsString("Corte de Cabelo")));
    }

    @Test
    @DisplayName("slug não resolvido pelo BusinessDirectory devolve 404")
    void slugNaoResolvidoDevolve404() throws Exception {
        mockMvc.perform(get("/b/slug-inexistente")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /b/{slug}/servicos/{serviceId} lista as ofertas do serviço")
    void profissionaisListaOfertas() throws Exception {
        var serviceId = UUID.randomUUID();
        var professionalId = UUID.randomUUID();
        when(serviceOfferingDirectory.listActiveByService(serviceId))
                .thenReturn(List.of(new PublicOfferingRef(offeringId, professionalId, "Maria", 30, "R$ 30,00")));

        mockMvc.perform(get("/b/{slug}/servicos/{serviceId}", SLUG, serviceId))
                .andExpect(status().isOk())
                .andExpect(view().name("public/profissionais"))
                .andExpect(content().string(Matchers.containsString("Maria")));
    }

    @Test
    @DisplayName("GET /b/{slug}/ofertas/{offeringId} lista os horários livres (reaproveita GetAvailableSlotsUseCase)")
    void horariosListaSlots() throws Exception {
        var professionalId = UUID.randomUUID();
        var inicio = LocalDateTime.of(2026, 9, 7, 8, 0);
        when(getAvailableSlots.handle(any()))
                .thenReturn(List.of(new AvailableSlot(professionalId, offeringId, inicio, inicio.plusMinutes(30))));

        mockMvc.perform(get("/b/{slug}/ofertas/{offeringId}", SLUG, offeringId).param("data", "2026-09-07"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/horarios"))
                .andExpect(content().string(Matchers.containsString("08:00")));
    }

    @Test
    @DisplayName("sem token CSRF, o POST de confirmação é recusado (AC-1)")
    void confirmarSemCsrfERecusado() throws Exception {
        mockMvc.perform(post("/b/{slug}/ofertas/{offeringId}", SLUG, offeringId)
                        .param("startsAt", "2026-09-07T08:00")
                        .param("data", "2026-09-07")
                        .param("name", "Joao")
                        .param("phone", "11988887777")
                        .param("website", ""))
                .andExpect(status().isForbidden());

        verify(bookAppointment, never()).handle(any());
    }

    @Test
    @DisplayName("honeypot preenchido não chama BookAppointmentUseCase e redireciona como sucesso")
    void honeypotPreenchidoNaoChamaCasoDeUso() throws Exception {
        mockMvc.perform(post("/b/{slug}/ofertas/{offeringId}", SLUG, offeringId)
                        .with(csrf())
                        .with(ipUnico())
                        .param("startsAt", "2026-09-07T08:00")
                        .param("data", "2026-09-07")
                        .param("name", "Bot")
                        .param("phone", "11900000000")
                        .param("website", "http://spam.example"))
                .andExpect(status().is3xxRedirection());

        verify(bookAppointment, never()).handle(any());
    }

    @Test
    @DisplayName("confirmação válida chama BookAppointmentUseCase e redireciona (PRG)")
    void confirmacaoValidaRedireciona() throws Exception {
        when(bookAppointment.handle(any()))
                .thenReturn(new BookedAppointment(
                        UUID.randomUUID(), "Corte de Cabelo", LocalDateTime.of(2026, 9, 7, 8, 0)));

        mockMvc.perform(post("/b/{slug}/ofertas/{offeringId}", SLUG, offeringId)
                        .with(csrf())
                        .with(ipUnico())
                        .param("startsAt", "2026-09-07T08:00")
                        .param("data", "2026-09-07")
                        .param("name", "Joao da Silva")
                        .param("phone", "11988887777")
                        .param("website", ""))
                .andExpect(status().is3xxRedirection());

        verify(bookAppointment).handle(any());
    }

    @Test
    @DisplayName("erro de negócio (ex.: horário já reservado) devolve a mesma tela com o erro, não 500")
    void erroDeNegocioDevolveATela() throws Exception {
        var professionalId = UUID.randomUUID();
        var inicio = LocalDateTime.of(2026, 9, 7, 8, 0);
        when(getAvailableSlots.handle(any()))
                .thenReturn(List.of(new AvailableSlot(professionalId, offeringId, inicio, inicio.plusMinutes(30))));
        when(bookAppointment.handle(any())).thenThrow(new SlotUnavailableException());

        mockMvc.perform(post("/b/{slug}/ofertas/{offeringId}", SLUG, offeringId)
                        .with(csrf())
                        .with(ipUnico())
                        .param("startsAt", "2026-09-07T08:00")
                        .param("data", "2026-09-07")
                        .param("name", "Joao da Silva")
                        .param("phone", "11988887777")
                        .param("website", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("public/horarios"))
                .andExpect(content().string(Matchers.containsString("acabou de ser reservado")));
    }

    @Test
    @DisplayName("6a tentativa do mesmo IP em 10 min é recusada por rate limit, antes de chamar o caso de uso (BR-8)")
    void sextaTentativaERecusadaPorRateLimit() throws Exception {
        var professionalId = UUID.randomUUID();
        var inicio = LocalDateTime.of(2026, 9, 7, 8, 0);
        when(getAvailableSlots.handle(any()))
                .thenReturn(List.of(new AvailableSlot(professionalId, offeringId, inicio, inicio.plusMinutes(30))));
        when(bookAppointment.handle(any()))
                .thenReturn(new BookedAppointment(UUID.randomUUID(), "Corte de Cabelo", inicio));

        var mesmoIp = ipUnico(); // as 6 tentativas precisam vir do MESMO IP simulado

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/b/{slug}/ofertas/{offeringId}", SLUG, offeringId)
                    .with(csrf())
                    .with(mesmoIp)
                    .param("startsAt", "2026-09-07T08:00")
                    .param("data", "2026-09-07")
                    .param("name", "Cliente " + i)
                    .param("phone", "1198888777" + i)
                    .param("website", ""));
        }

        mockMvc.perform(post("/b/{slug}/ofertas/{offeringId}", SLUG, offeringId)
                        .with(csrf())
                        .with(mesmoIp)
                        .param("startsAt", "2026-09-07T09:00")
                        .param("data", "2026-09-07")
                        .param("name", "Sexto Cliente")
                        .param("phone", "11999998888")
                        .param("website", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Muitas tentativas")));

        verify(bookAppointment, org.mockito.Mockito.times(5)).handle(any());
    }
}
