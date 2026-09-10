package com.simboraagendar.scheduling.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.simboraagendar.catalog.api.ActiveOfferingRef;
import com.simboraagendar.catalog.api.ServiceOfferingDirectory;
import com.simboraagendar.organization.api.ProfessionalDirectory;
import com.simboraagendar.organization.api.ProfessionalRef;
import com.simboraagendar.platform.security.SecurityConfig;
import com.simboraagendar.scheduling.application.port.in.AgendaEntry;
import com.simboraagendar.scheduling.application.port.in.AppointmentDetails;
import com.simboraagendar.scheduling.application.port.in.AppointmentDetailsUseCase;
import com.simboraagendar.scheduling.application.port.in.BookAppointmentCommand;
import com.simboraagendar.scheduling.application.port.in.BookedAppointment;
import com.simboraagendar.scheduling.application.port.in.CancelAppointmentByOwnerUseCase;
import com.simboraagendar.scheduling.application.port.in.CompleteAppointmentUseCase;
import com.simboraagendar.scheduling.application.port.in.ConfirmAppointmentUseCase;
import com.simboraagendar.scheduling.application.port.in.CreateAppointmentManuallyUseCase;
import com.simboraagendar.scheduling.application.port.in.RescheduleAppointmentUseCase;
import com.simboraagendar.scheduling.application.port.in.UpdatePaymentStatusUseCase;
import com.simboraagendar.scheduling.application.port.in.ViewAgendaUseCase;
import com.simboraagendar.scheduling.domain.AppointmentStatus;
import com.simboraagendar.scheduling.domain.PaymentStatus;
import com.simboraagendar.scheduling.domain.exception.AppointmentNotFoundException;
import com.simboraagendar.shared.UuidV7;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Camada web isolada: os casos de uso e os directories são mock, o banco não existe. */
@WebMvcTest(AgendaController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "OWNER")
class AgendaControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ViewAgendaUseCase viewAgenda;
    @MockitoBean private CreateAppointmentManuallyUseCase createAppointmentManually;
    @MockitoBean private ConfirmAppointmentUseCase confirmAppointment;
    @MockitoBean private CancelAppointmentByOwnerUseCase cancelAppointmentByOwner;
    @MockitoBean private RescheduleAppointmentUseCase rescheduleAppointment;
    @MockitoBean private CompleteAppointmentUseCase completeAppointment;
    @MockitoBean private UpdatePaymentStatusUseCase updatePaymentStatus;
    @MockitoBean private AppointmentDetailsUseCase appointmentDetails;
    @MockitoBean private ProfessionalDirectory professionalDirectory;
    @MockitoBean private ServiceOfferingDirectory serviceOfferingDirectory;

    private final UUID professionalId = UuidV7.generate();
    private final UUID serviceOfferingId = UuidV7.generate();
    private final UUID appointmentId = UuidV7.generate();
    private final Instant startsAt = Instant.now().plus(1, ChronoUnit.DAYS);

    private List<ActiveOfferingRef> umaOferta() {
        return List.of(new ActiveOfferingRef(
                serviceOfferingId, professionalId, "Maria", "Corte de Cabelo", 30, "R$ 30,00"));
    }

    private AppointmentDetails detalhes() {
        return new AppointmentDetails(
                appointmentId,
                "Maria",
                "João",
                "Corte de Cabelo",
                startsAt,
                startsAt.plus(30, ChronoUnit.MINUTES),
                AppointmentStatus.SCHEDULED,
                true,
                true);
    }

    @Test
    @DisplayName("sem sessao, qualquer rota de /admin/agenda manda para o login (AC-1)")
    void semSessaoExigeLogin() throws Exception {
        mockMvc.perform(get("/admin/agenda").with(anonymous())).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /admin/agenda lista os agendamentos do profissional escolhido")
    void listaAgendaDoDia() throws Exception {
        when(professionalDirectory.listActive())
                .thenReturn(List.of(new ProfessionalRef(professionalId, "Maria")));
        when(viewAgenda.handle(eq(professionalId), any(LocalDate.class)))
                .thenReturn(List.of(new AgendaEntry(
                        appointmentId,
                        "João",
                        "+5511999990000",
                        "Corte de Cabelo",
                        startsAt,
                        startsAt.plus(30, ChronoUnit.MINUTES),
                        AppointmentStatus.SCHEDULED,
                        PaymentStatus.PENDING,
                        true,
                        true,
                        true,
                        false)));

        mockMvc.perform(get("/admin/agenda").param("professionalId", professionalId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/agenda"))
                .andExpect(content().string(Matchers.containsString("João")))
                .andExpect(content().string(Matchers.containsString("Corte de Cabelo")));
    }

    @Test
    @DisplayName("GET /admin/agenda/novo mostra o formulario com as ofertas ativas")
    void mostraFormularioDeNovoAgendamento() throws Exception {
        when(serviceOfferingDirectory.listActive()).thenReturn(umaOferta());

        mockMvc.perform(get("/admin/agenda/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/agenda-novo"))
                .andExpect(model().attributeExists("form"))
                .andExpect(content().string(Matchers.containsString("Corte de Cabelo")));
    }

    @Test
    @DisplayName("POST /admin/agenda/novo valido cria e redireciona (PRG)")
    void criarAgendamentoValido() throws Exception {
        when(createAppointmentManually.create(any(BookAppointmentCommand.class)))
                .thenReturn(new BookedAppointment(appointmentId, "Corte de Cabelo", LocalDateTime.now()));

        mockMvc.perform(post("/admin/agenda/novo")
                        .with(csrf())
                        .param("serviceOfferingId", serviceOfferingId.toString())
                        .param("date", LocalDate.now().plusDays(1).toString())
                        .param("time", "10:00")
                        .param("customerName", "João")
                        .param("customerPhone", "+5511999990000"))
                .andExpect(status().is3xxRedirection());

        verify(createAppointmentManually).create(any(BookAppointmentCommand.class));
    }

    @Test
    @DisplayName("POST /admin/agenda/novo invalido nao chama o caso de uso, recarrega o formulario")
    void criarAgendamentoInvalido() throws Exception {
        when(serviceOfferingDirectory.listActive()).thenReturn(umaOferta());

        mockMvc.perform(post("/admin/agenda/novo").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/agenda-novo"))
                .andExpect(model()
                        .attributeHasFieldErrors(
                                "form", "serviceOfferingId", "date", "time", "customerName", "customerPhone"));

        verify(createAppointmentManually, never()).create(any());
    }

    @Test
    @DisplayName("POST confirmar chama ConfirmAppointmentUseCase e redireciona")
    void confirmarChamaCasoDeUso() throws Exception {
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/confirmar", appointmentId).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(confirmAppointment).confirm(appointmentId);
    }

    @Test
    @DisplayName("POST confirmar com id de outro tenant devolve 404 (AC-2)")
    void confirmarComIdDeOutroTenantDevolve404() throws Exception {
        doThrow(new AppointmentNotFoundException()).when(confirmAppointment).confirm(appointmentId);

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/confirmar", appointmentId).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST cancelar chama CancelAppointmentByOwnerUseCase e redireciona")
    void cancelarChamaCasoDeUso() throws Exception {
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/cancelar", appointmentId).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(cancelAppointmentByOwner).cancel(appointmentId);
    }

    @Test
    @DisplayName("POST cancelar com id de outro tenant devolve 404 (AC-2)")
    void cancelarComIdDeOutroTenantDevolve404() throws Exception {
        doThrow(new AppointmentNotFoundException()).when(cancelAppointmentByOwner).cancel(appointmentId);

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/cancelar", appointmentId).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST concluir chama CompleteAppointmentUseCase e redireciona")
    void concluirChamaCasoDeUso() throws Exception {
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/concluir", appointmentId).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(completeAppointment).complete(appointmentId);
    }

    @Test
    @DisplayName("POST concluir com id de outro tenant devolve 404 (AC-2)")
    void concluirComIdDeOutroTenantDevolve404() throws Exception {
        doThrow(new AppointmentNotFoundException()).when(completeAppointment).complete(appointmentId);

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/concluir", appointmentId).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST concluir sem token CSRF e recusado")
    void concluirSemCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/concluir", appointmentId))
                .andExpect(status().isForbidden());

        verify(completeAppointment, never()).complete(any());
    }

    @Test
    @DisplayName("POST pagamento chama UpdatePaymentStatusUseCase e redireciona (PRG)")
    void pagamentoChamaCasoDeUso() throws Exception {
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/pagamento", appointmentId)
                        .with(csrf())
                        .param("status", "ON_CREDIT"))
                .andExpect(status().is3xxRedirection());

        verify(updatePaymentStatus).updatePaymentStatus(appointmentId, PaymentStatus.ON_CREDIT);
    }

    @Test
    @DisplayName("POST pagamento com id de outro tenant devolve 404 (AC-5)")
    void pagamentoComIdDeOutroTenantDevolve404() throws Exception {
        doThrow(new AppointmentNotFoundException())
                .when(updatePaymentStatus)
                .updatePaymentStatus(appointmentId, PaymentStatus.PAID);

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/pagamento", appointmentId)
                        .with(csrf())
                        .param("status", "PAID"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST pagamento com status fora do enum devolve 400, nao 500 (GlobalExceptionHandler)")
    void pagamentoComStatusInvalidoDevolve400() throws Exception {
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/pagamento", appointmentId)
                        .with(csrf())
                        .param("status", "XPTO"))
                .andExpect(status().isBadRequest());

        verify(updatePaymentStatus, never()).updatePaymentStatus(any(), any());
    }

    @Test
    @DisplayName("POST pagamento sem token CSRF e recusado")
    void pagamentoSemCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/pagamento", appointmentId).param("status", "PAID"))
                .andExpect(status().isForbidden());

        verify(updatePaymentStatus, never()).updatePaymentStatus(any(), any());
    }

    @Test
    @DisplayName("GET reagendar mostra o agendamento atual e o formulario")
    void mostraFormularioDeReagendamento() throws Exception {
        when(appointmentDetails.handle(appointmentId)).thenReturn(detalhes());
        when(serviceOfferingDirectory.listActive()).thenReturn(umaOferta());

        mockMvc.perform(get("/admin/agenda/agendamentos/{id}/reagendar", appointmentId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/agenda-reagendar"))
                .andExpect(content().string(Matchers.containsString("João")));
    }

    @Test
    @DisplayName("GET reagendar com id de outro tenant devolve 404 (AC-2)")
    void reagendarComIdDeOutroTenantDevolve404() throws Exception {
        when(appointmentDetails.handle(appointmentId)).thenThrow(new AppointmentNotFoundException());

        mockMvc.perform(get("/admin/agenda/agendamentos/{id}/reagendar", appointmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST reagendar valido reagenda e redireciona (PRG)")
    void reagendarValido() throws Exception {
        when(rescheduleAppointment.reschedule(any()))
                .thenReturn(new BookedAppointment(appointmentId, "Corte de Cabelo", LocalDateTime.now()));

        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/reagendar", appointmentId)
                        .with(csrf())
                        .param("serviceOfferingId", serviceOfferingId.toString())
                        .param("date", LocalDate.now().plusDays(1).toString())
                        .param("time", "10:00"))
                .andExpect(status().is3xxRedirection());

        verify(rescheduleAppointment).reschedule(any());
    }

    @Test
    @DisplayName("POST sem token CSRF e recusado em qualquer rota de acao")
    void semCsrfERecusado() throws Exception {
        mockMvc.perform(post("/admin/agenda/agendamentos/{id}/confirmar", appointmentId))
                .andExpect(status().isForbidden());

        verify(confirmAppointment, never()).confirm(any());
    }
}
