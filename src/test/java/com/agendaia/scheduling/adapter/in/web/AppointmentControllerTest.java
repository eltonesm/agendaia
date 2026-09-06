package com.agendaia.scheduling.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.agendaia.organization.api.BusinessDirectory;
import com.agendaia.organization.api.BusinessRef;
import com.agendaia.platform.security.SecurityConfig;
import com.agendaia.scheduling.application.port.in.AppointmentDetails;
import com.agendaia.scheduling.application.port.in.AppointmentDetailsUseCase;
import com.agendaia.scheduling.application.port.in.CancelAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.ConfirmAppointmentUseCase;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.exception.AppointmentNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Camada web isolada: as três portas são mock, o banco não existe.
 *
 * <p>{@code BusinessDirectory} mockado para o {@code TenantContextFilter}
 * real (entra nesta fatia por ser {@code @Component} global) resolver o
 * tenant pelo slug, mesma técnica de {@code PublicBookingControllerTest}.
 */
@WebMvcTest(AppointmentController.class)
@Import(SecurityConfig.class)
class AppointmentControllerTest {

    private static final String SLUG = "barbearia-teste";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AppointmentDetailsUseCase appointmentDetails;
    @MockitoBean private ConfirmAppointmentUseCase confirmAppointment;
    @MockitoBean private CancelAppointmentUseCase cancelAppointment;
    @MockitoBean private BusinessDirectory businessDirectory;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();
    private final Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);

    private void resolverTenantPeloSlug(String whatsapp) {
        when(businessDirectory.findBySlug(SLUG))
                .thenReturn(Optional.of(new BusinessRef(tenantId, "Barbearia Teste", SLUG, whatsapp, Instant.now())));
    }

    private AppointmentDetails detalhesValidos(boolean canConfirm, boolean canCancel) {
        return new AppointmentDetails(
                appointmentId,
                "Maria",
                "João da Silva",
                "Corte de Cabelo",
                startsAt,
                startsAt.plus(30, ChronoUnit.MINUTES),
                AppointmentStatus.SCHEDULED,
                canConfirm,
                canCancel);
    }

    @Test
    @DisplayName("GET mostra os detalhes e os botões disponíveis conforme canConfirm/canCancel")
    void verMostraDetalhes() throws Exception {
        resolverTenantPeloSlug(null);
        when(appointmentDetails.handle(appointmentId)).thenReturn(detalhesValidos(true, true));

        mockMvc.perform(get("/b/{slug}/agendamentos/{id}", SLUG, appointmentId))
                .andExpect(status().isOk())
                .andExpect(view().name("public/agendamento"))
                .andExpect(content().string(Matchers.containsString("Corte de Cabelo")))
                .andExpect(content().string(Matchers.containsString("Confirmar presença")))
                .andExpect(content().string(Matchers.containsString("Cancelar")));
    }

    @Test
    @DisplayName("GET com id de outro tenant (ou inexistente) devolve 404, não 422 (AC-1)")
    void verComIdDeOutroTenantDevolve404() throws Exception {
        resolverTenantPeloSlug(null);
        when(appointmentDetails.handle(appointmentId)).thenThrow(new AppointmentNotFoundException());

        mockMvc.perform(get("/b/{slug}/agendamentos/{id}", SLUG, appointmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("slug não resolvido pelo BusinessDirectory devolve 404")
    void slugNaoResolvidoDevolve404() throws Exception {
        mockMvc.perform(get("/b/slug-inexistente/agendamentos/{id}", appointmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sem token CSRF, o POST de confirmar é recusado")
    void confirmarSemCsrfERecusado() throws Exception {
        resolverTenantPeloSlug(null);

        mockMvc.perform(post("/b/{slug}/agendamentos/{id}/confirmar", SLUG, appointmentId))
                .andExpect(status().isForbidden());

        verify(confirmAppointment, never()).confirm(any());
    }

    @Test
    @DisplayName("POST confirmar chama ConfirmAppointmentUseCase e redireciona (PRG) (AC-2)")
    void confirmarChamaCasoDeUso() throws Exception {
        resolverTenantPeloSlug(null);

        mockMvc.perform(post("/b/{slug}/agendamentos/{id}/confirmar", SLUG, appointmentId).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(confirmAppointment).confirm(appointmentId);
    }

    @Test
    @DisplayName("POST confirmar com id de outro tenant devolve 404, nunca chama o caso de uso normalmente")
    void confirmarComIdDeOutroTenantDevolve404() throws Exception {
        resolverTenantPeloSlug(null);
        org.mockito.Mockito.doThrow(new AppointmentNotFoundException())
                .when(confirmAppointment)
                .confirm(appointmentId);

        mockMvc.perform(post("/b/{slug}/agendamentos/{id}/confirmar", SLUG, appointmentId).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST cancelar chama CancelAppointmentUseCase e redireciona (PRG) (AC-3)")
    void cancelarChamaCasoDeUso() throws Exception {
        resolverTenantPeloSlug(null);

        mockMvc.perform(post("/b/{slug}/agendamentos/{id}/cancelar", SLUG, appointmentId).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(cancelAppointment).cancel(appointmentId);
    }

    @Test
    @DisplayName("GET .../ics devolve text/calendar sem nome nem telefone do cliente (BR-9)")
    void icsNaoCarregaDadoDoCliente() throws Exception {
        resolverTenantPeloSlug(null);
        when(appointmentDetails.handle(appointmentId)).thenReturn(detalhesValidos(true, true));

        mockMvc.perform(get("/b/{slug}/agendamentos/{id}/ics", SLUG, appointmentId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("text/calendar")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("agendamento.ics")))
                .andExpect(content().string(Matchers.containsString("Corte de Cabelo")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("João da Silva"))));
    }

    @Test
    @DisplayName("whatsappLink aparece só quando o estabelecimento tem WhatsApp cadastrado (BR-8)")
    void whatsappLinkCondicional() throws Exception {
        resolverTenantPeloSlug("5511988887777");
        when(appointmentDetails.handle(appointmentId)).thenReturn(detalhesValidos(true, true));

        mockMvc.perform(get("/b/{slug}/agendamentos/{id}", SLUG, appointmentId))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("wa.me/5511988887777")));
    }

    @Test
    @DisplayName("sem WhatsApp cadastrado, a tela não mostra nenhum link wa.me")
    void semWhatsappSemLink() throws Exception {
        resolverTenantPeloSlug(null);
        when(appointmentDetails.handle(appointmentId)).thenReturn(detalhesValidos(true, true));

        mockMvc.perform(get("/b/{slug}/agendamentos/{id}", SLUG, appointmentId))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("wa.me"))));
    }
}
