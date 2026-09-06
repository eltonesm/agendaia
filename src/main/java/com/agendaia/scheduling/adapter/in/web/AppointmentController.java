package com.agendaia.scheduling.adapter.in.web;

import com.agendaia.organization.api.BusinessRef;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.platform.tenant.TenantContextFilter;
import com.agendaia.scheduling.application.port.in.AppointmentDetails;
import com.agendaia.scheduling.application.port.in.AppointmentDetailsUseCase;
import com.agendaia.scheduling.application.port.in.CancelAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.ConfirmAppointmentUseCase;
import com.agendaia.scheduling.domain.exception.AppointmentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Governa o agendamento já criado, pelo link (confirmacao-e-cancelamento,
 * TODO-007) — ver, confirmar presença, cancelar, baixar {@code .ics} e
 * falar pelo WhatsApp do estabelecimento.
 *
 * <p>Separado de {@link PublicBookingController} (DD-5 da spec técnica):
 * os dois fluxos (reservar vs. gerenciar o que já existe) não compartilham
 * dependência nenhuma. O tenant não vem do id do agendamento — vem do
 * {@code slug} da URL, já resolvido pelo {@code TenantContextFilter} antes
 * deste controller rodar, mesma garantia de {@link PublicBookingController}.
 */
@Controller
@RequestMapping("/b/{slug}/agendamentos/{id}")
public class AppointmentController {

    private static final String VIEW_AGENDAMENTO = "public/agendamento";

    private final AppointmentDetailsUseCase appointmentDetails;
    private final ConfirmAppointmentUseCase confirmAppointment;
    private final CancelAppointmentUseCase cancelAppointment;

    public AppointmentController(
            AppointmentDetailsUseCase appointmentDetails,
            ConfirmAppointmentUseCase confirmAppointment,
            CancelAppointmentUseCase cancelAppointment) {
        this.appointmentDetails = appointmentDetails;
        this.confirmAppointment = confirmAppointment;
        this.cancelAppointment = cancelAppointment;
    }

    @GetMapping
    public String ver(@PathVariable String slug, @PathVariable UUID id, HttpServletRequest request, Model model) {
        exigirTenantResolvido();
        var detalhes = detalhesOuFalhar(id);

        model.addAttribute("slug", slug);
        model.addAttribute("agendamento", detalhes);
        model.addAttribute("whatsappLink", linkWhatsapp(request, detalhes.customerName(), detalhes.startsAt()));
        return VIEW_AGENDAMENTO;
    }

    @PostMapping("/confirmar")
    public String confirmar(@PathVariable String slug, @PathVariable UUID id) {
        exigirTenantResolvido();
        executarOuFalhar(() -> confirmAppointment.confirm(id));
        return "redirect:/b/{slug}/agendamentos/{id}";
    }

    @PostMapping("/cancelar")
    public String cancelar(@PathVariable String slug, @PathVariable UUID id) {
        exigirTenantResolvido();
        executarOuFalhar(() -> cancelAppointment.cancel(id));
        return "redirect:/b/{slug}/agendamentos/{id}";
    }

    @GetMapping("/ics")
    public ResponseEntity<String> ics(@PathVariable String slug, @PathVariable UUID id, HttpServletRequest request) {
        exigirTenantResolvido();
        var detalhes = detalhesOuFalhar(id);
        var negocio = negocioResolvido(request);
        var corpo = IcsWriter.escrever(
                detalhes.serviceName(), negocio.name(), detalhes.startsAt(), detalhes.endsAt());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header("Content-Disposition", "attachment; filename=\"agendamento.ics\"")
                .body(corpo);
    }

    /**
     * Id de outro tenant ou inexistente vira 404 — mesmo tratamento de
     * {@code ServiceOfferingNotFoundException} em {@link PublicBookingController}
     * (DD-1 da spec técnica desta feature), nunca o 422 genérico do
     * {@code GlobalExceptionHandler}.
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

    /**
     * {@code null} quando o estabelecimento não cadastrou WhatsApp (BR-8) —
     * o template omite o link nesse caso, sem erro.
     */
    private String linkWhatsapp(HttpServletRequest request, String customerName, Instant startsAt) {
        var negocio = negocioResolvido(request);
        if (negocio.whatsapp() == null) {
            return null;
        }
        var mensagem = "Olá, sou %s, tenho um agendamento em %s e preciso falar sobre ele."
                .formatted(customerName, startsAt);
        var mensagemCodificada = URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
        return "https://wa.me/" + negocio.whatsapp() + "?text=" + mensagemCodificada;
    }

    private static BusinessRef negocioResolvido(HttpServletRequest request) {
        var atributo = request.getAttribute(TenantContextFilter.REQUEST_ATTR_RESOLVED_BUSINESS);
        if (atributo instanceof BusinessRef negocio) {
            return negocio;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    /** Não é o filtro que decide 404 (DD-3 de pagina-publica-agendamento): rotear é do controller. */
    private static void exigirTenantResolvido() {
        if (TenantContext.current().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
