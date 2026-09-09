package com.agendaia.scheduling.adapter.in.web;

import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.scheduling.adapter.in.web.request.ManualAppointmentRequest;
import com.agendaia.scheduling.adapter.in.web.request.RescheduleRequest;
import com.agendaia.scheduling.application.port.in.AppointmentDetails;
import com.agendaia.scheduling.application.port.in.AppointmentDetailsUseCase;
import com.agendaia.scheduling.application.port.in.BookAppointmentCommand;
import com.agendaia.scheduling.application.port.in.CancelAppointmentByOwnerUseCase;
import com.agendaia.scheduling.application.port.in.CompleteAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.ConfirmAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.CreateAppointmentManuallyUseCase;
import com.agendaia.scheduling.application.port.in.RescheduleAppointmentCommand;
import com.agendaia.scheduling.application.port.in.RescheduleAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.ViewAgendaUseCase;
import com.agendaia.scheduling.domain.exception.AppointmentNotFoundException;
import com.agendaia.shared.DomainException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Agenda do dono sobre o agendamento (agenda-profissional, TODO-008): ver a
 * agenda do dia, criar manualmente, confirmar, cancelar e reagendar.
 *
 * <p>Sem configuração nova em {@code SecurityConfig}: {@code /admin/**} já
 * cai em {@code anyRequest().hasRole("OWNER")} por omissão (DD-1). O tenant
 * vem da sessão autenticada, via {@code TenantContext}, resolvido pelo
 * {@code TenantContextFilter} antes deste controller rodar — nunca de
 * parâmetro de formulário.
 */
@Controller
@RequestMapping("/admin/agenda")
public class AgendaController {

    private static final String VIEW_AGENDA = "admin/agenda";
    private static final String VIEW_NOVO = "admin/agenda-novo";
    private static final String VIEW_REAGENDAR = "admin/agenda-reagendar";

    private final ViewAgendaUseCase viewAgenda;
    private final CreateAppointmentManuallyUseCase createAppointmentManually;
    private final ConfirmAppointmentUseCase confirmAppointment;
    private final CancelAppointmentByOwnerUseCase cancelAppointmentByOwner;
    private final RescheduleAppointmentUseCase rescheduleAppointment;
    private final CompleteAppointmentUseCase completeAppointment;
    private final AppointmentDetailsUseCase appointmentDetails;
    private final ProfessionalDirectory professionalDirectory;
    private final ServiceOfferingDirectory serviceOfferingDirectory;

    public AgendaController(
            ViewAgendaUseCase viewAgenda,
            CreateAppointmentManuallyUseCase createAppointmentManually,
            ConfirmAppointmentUseCase confirmAppointment,
            CancelAppointmentByOwnerUseCase cancelAppointmentByOwner,
            RescheduleAppointmentUseCase rescheduleAppointment,
            CompleteAppointmentUseCase completeAppointment,
            AppointmentDetailsUseCase appointmentDetails,
            ProfessionalDirectory professionalDirectory,
            ServiceOfferingDirectory serviceOfferingDirectory) {
        this.viewAgenda = viewAgenda;
        this.createAppointmentManually = createAppointmentManually;
        this.confirmAppointment = confirmAppointment;
        this.cancelAppointmentByOwner = cancelAppointmentByOwner;
        this.rescheduleAppointment = rescheduleAppointment;
        this.completeAppointment = completeAppointment;
        this.appointmentDetails = appointmentDetails;
        this.professionalDirectory = professionalDirectory;
        this.serviceOfferingDirectory = serviceOfferingDirectory;
    }

    @GetMapping
    public String ver(
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) LocalDate date,
            Model model) {
        var profissionais = professionalDirectory.listActive();
        var profissionalEscolhido = professionalId != null
                ? professionalId
                : profissionais.stream().findFirst().map(p -> p.id()).orElse(null);
        var dataEscolhida = date != null ? date : LocalDate.now();

        model.addAttribute("profissionais", profissionais);
        model.addAttribute("professionalId", profissionalEscolhido);
        model.addAttribute("date", dataEscolhida);
        model.addAttribute(
                "agendamentos",
                profissionalEscolhido == null ? List.of() : viewAgenda.handle(profissionalEscolhido, dataEscolhida));
        return VIEW_AGENDA;
    }

    @GetMapping("/novo")
    public String novo(
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) LocalDate date,
            Model model) {
        model.addAttribute("form", new ManualAppointmentRequest());
        model.addAttribute("professionalId", professionalId);
        model.addAttribute("date", date != null ? date : LocalDate.now());
        carregarOfertas(model);
        return VIEW_NOVO;
    }

    @PostMapping("/novo")
    public String criar(
            @Valid @ModelAttribute("form") ManualAppointmentRequest form, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            carregarOfertas(model);
            return VIEW_NOVO;
        }

        var startsAt = LocalDateTime.of(form.date(), form.time())
                .atZone(ZoneId.systemDefault())
                .toInstant();

        try {
            createAppointmentManually.create(
                    new BookAppointmentCommand(form.serviceOfferingId(), startsAt, form.customerName(), form.customerPhone()));
            return "redirect:/admin/agenda?date=" + form.date();
        } catch (DomainException e) {
            if (e.hasField()) {
                binding.rejectValue(e.field(), "invalido", e.getMessage());
            } else {
                binding.reject("erro", e.getMessage());
            }
            carregarOfertas(model);
            return VIEW_NOVO;
        }
    }

    @PostMapping("/agendamentos/{id}/confirmar")
    public String confirmar(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) LocalDate date) {
        executarOuFalhar(() -> confirmAppointment.confirm(id));
        return redirecionarParaAgenda(professionalId, date);
    }

    @PostMapping("/agendamentos/{id}/cancelar")
    public String cancelar(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) LocalDate date) {
        executarOuFalhar(() -> cancelAppointmentByOwner.cancel(id));
        return redirecionarParaAgenda(professionalId, date);
    }

    @PostMapping("/agendamentos/{id}/concluir")
    public String concluir(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) LocalDate date) {
        executarOuFalhar(() -> completeAppointment.complete(id));
        return redirecionarParaAgenda(professionalId, date);
    }

    @GetMapping("/agendamentos/{id}/reagendar")
    public String reagendar(@PathVariable UUID id, Model model) {
        var detalhes = detalhesOuFalhar(id);
        model.addAttribute("agendamento", detalhes);
        model.addAttribute("form", new RescheduleRequest());
        carregarOfertas(model);
        return VIEW_REAGENDAR;
    }

    @PostMapping("/agendamentos/{id}/reagendar")
    public String reagendarConfirmar(
            @PathVariable UUID id, @Valid @ModelAttribute("form") RescheduleRequest form, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("agendamento", detalhesOuFalhar(id));
            carregarOfertas(model);
            return VIEW_REAGENDAR;
        }

        var novoInicio = LocalDateTime.of(form.date(), form.time())
                .atZone(ZoneId.systemDefault())
                .toInstant();

        try {
            rescheduleAppointment.reschedule(new RescheduleAppointmentCommand(id, form.serviceOfferingId(), novoInicio));
            return "redirect:/admin/agenda?date=" + form.date();
        } catch (AppointmentNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (DomainException e) {
            if (e.hasField()) {
                binding.rejectValue(e.field(), "invalido", e.getMessage());
            } else {
                binding.reject("erro", e.getMessage());
            }
            model.addAttribute("agendamento", detalhesOuFalhar(id));
            carregarOfertas(model);
            return VIEW_REAGENDAR;
        }
    }

    private void carregarOfertas(Model model) {
        model.addAttribute("ofertas", serviceOfferingDirectory.listActive());
    }

    private String redirecionarParaAgenda(UUID professionalId, LocalDate date) {
        var dataEscolhida = date != null ? date : LocalDate.now();
        if (professionalId == null) {
            return "redirect:/admin/agenda?date=" + dataEscolhida;
        }
        return "redirect:/admin/agenda?professionalId=" + professionalId + "&date=" + dataEscolhida;
    }

    /**
     * Id de outro tenant ou inexistente vira 404 — mesmo tratamento de
     * {@code AppointmentController} (confirmacao-e-cancelamento, TODO-007).
     */
    private AppointmentDetails detalhesOuFalhar(UUID id) {
        try {
            return appointmentDetails.handle(id);
        } catch (AppointmentNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static void executarOuFalhar(Runnable acao) {
        try {
            acao.run();
        } catch (AppointmentNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
