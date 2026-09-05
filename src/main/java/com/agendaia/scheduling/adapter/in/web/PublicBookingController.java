package com.agendaia.scheduling.adapter.in.web;

import com.agendaia.catalog.api.ServiceDirectory;
import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.adapter.in.web.request.PublicBookingRequest;
import com.agendaia.scheduling.application.port.in.BookAppointmentCommand;
import com.agendaia.scheduling.application.port.in.BookAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.GetAvailableSlotsQuery;
import com.agendaia.scheduling.application.port.in.GetAvailableSlotsUseCase;
import com.agendaia.shared.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;

/**
 * Página pública de agendamento (pagina-publica-agendamento, TODO-006).
 *
 * <p>O tenant não vem do {@code {slug}} da URL — vem do
 * {@link TenantContext}, já resolvido pelo {@code TenantContextFilter}
 * antes deste controller rodar (DD-3 da spec técnica). O {@code slug} no
 * path é só cosmético para o link ficar legível; se o filtro não
 * resolveu tenant nenhum, todo método aqui devolve 404 — nunca tenta
 * adivinhar ou aceitar um tenant vindo de outro lugar.
 */
@Controller
@RequestMapping("/b/{slug}")
public class PublicBookingController {

    private static final String VIEW_CATALOGO = "public/catalogo";
    private static final String VIEW_PROFISSIONAIS = "public/profissionais";
    private static final String VIEW_HORARIOS = "public/horarios";
    private static final String VIEW_SUCESSO = "public/sucesso";

    private final ServiceDirectory serviceDirectory;
    private final ServiceOfferingDirectory serviceOfferingDirectory;
    private final GetAvailableSlotsUseCase getAvailableSlots;
    private final BookAppointmentUseCase bookAppointment;
    private final BookingRateLimiter rateLimiter;

    public PublicBookingController(
            ServiceDirectory serviceDirectory,
            ServiceOfferingDirectory serviceOfferingDirectory,
            GetAvailableSlotsUseCase getAvailableSlots,
            BookAppointmentUseCase bookAppointment,
            BookingRateLimiter rateLimiter) {
        this.serviceDirectory = serviceDirectory;
        this.serviceOfferingDirectory = serviceOfferingDirectory;
        this.getAvailableSlots = getAvailableSlots;
        this.bookAppointment = bookAppointment;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public String catalogo(@PathVariable String slug, Model model) {
        exigirTenantResolvido();
        model.addAttribute("slug", slug);
        model.addAttribute("servicos", serviceDirectory.listActive());
        return VIEW_CATALOGO;
    }

    @GetMapping("/servicos/{serviceId}")
    public String profissionais(@PathVariable String slug, @PathVariable UUID serviceId, Model model) {
        exigirTenantResolvido();
        model.addAttribute("slug", slug);
        model.addAttribute("serviceId", serviceId);
        model.addAttribute("ofertas", serviceOfferingDirectory.listActiveByService(serviceId));
        return VIEW_PROFISSIONAIS;
    }

    @GetMapping("/ofertas/{offeringId}")
    public String horarios(
            @PathVariable String slug,
            @PathVariable UUID offeringId,
            @RequestParam(name = "data", required = false) LocalDate data,
            Model model) {
        exigirTenantResolvido();
        var dataConsultada = data != null ? data : LocalDate.now();

        model.addAttribute("slug", slug);
        model.addAttribute("offeringId", offeringId);
        model.addAttribute("data", dataConsultada);
        model.addAttribute(
                "horarios", getAvailableSlots.handle(new GetAvailableSlotsQuery(offeringId, dataConsultada)));
        model.addAttribute("form", new PublicBookingRequest());
        return VIEW_HORARIOS;
    }

    @PostMapping("/ofertas/{offeringId}")
    public String confirmar(
            @PathVariable String slug,
            @PathVariable UUID offeringId,
            @RequestParam("startsAt") String startsAtParam,
            @RequestParam(name = "data", required = false) LocalDate data,
            @Valid @ModelAttribute("form") PublicBookingRequest form,
            BindingResult binding,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        exigirTenantResolvido();

        // A tela recarregada em caso de erro precisa continuar mostrando a
        // MESMA data que o cliente escolheu, nunca "hoje" — sem isso, um
        // erro em qualquer data diferente de hoje faz a lista de horários
        // sumir (achado durante o teste manual do TASK-010).
        var dataDoFormulario = data != null ? data : LocalDate.now();

        // BR-7: honeypot preenchido — resposta indistinguível de sucesso,
        // nunca revela ao remetente que foi identificado como bot.
        if (form.isHoneypotFilled()) {
            return "redirect:/b/{slug}";
        }

        // BR-8: rate limit por IP, antes de qualquer outra validação.
        if (!rateLimiter.tryAcquire(enderecoRemoto(request))) {
            binding.reject("rateLimit", "Muitas tentativas. Aguarde alguns minutos e tente novamente.");
            return recarregarTelaDeHorarios(slug, offeringId, dataDoFormulario, model);
        }

        if (binding.hasErrors()) {
            return recarregarTelaDeHorarios(slug, offeringId, dataDoFormulario, model);
        }

        var startsAt =
                LocalDateTime.parse(startsAtParam).atZone(ZoneId.systemDefault()).toInstant();

        try {
            var agendado = bookAppointment.handle(
                    new BookAppointmentCommand(offeringId, startsAt, form.name(), form.phone()));
            // Flash attribute: sobrevive só ao próximo GET (PRG), sem nova
            // consulta ao banco e sem expor o resumo na querystring (US-5).
            redirectAttributes.addFlashAttribute("serviceName", agendado.serviceName());
            redirectAttributes.addFlashAttribute("startsAt", agendado.startsAt());
            return "redirect:/b/{slug}/agendamentos/" + agendado.id();
        } catch (DomainException e) {
            if (e.hasField()) {
                binding.rejectValue(e.field(), "invalido", e.getMessage());
            } else {
                binding.reject("erro", e.getMessage());
            }
            return recarregarTelaDeHorarios(slug, offeringId, dataDoFormulario, model);
        }
    }

    @GetMapping("/agendamentos/{id}")
    public String sucesso(@PathVariable String slug, @PathVariable UUID id, Model model) {
        exigirTenantResolvido();
        // O resumo (serviceName/startsAt) chega via flash attribute do POST
        // que criou o agendamento — se a página for recarregada depois, os
        // dois somem, e a tela mostra só a confirmação genérica com o id.
        // Nome do estabelecimento já vem do LayoutAdvice.
        model.addAttribute("slug", slug);
        model.addAttribute("appointmentId", id);
        return VIEW_SUCESSO;
    }

    /** Não é o filtro que decide 404 (DD-3): rotear é responsabilidade do controller. */
    private static void exigirTenantResolvido() {
        if (TenantContext.current().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private String recarregarTelaDeHorarios(String slug, UUID offeringId, LocalDate data, Model model) {
        model.addAttribute("slug", slug);
        model.addAttribute("offeringId", offeringId);
        model.addAttribute("data", data);
        model.addAttribute("horarios", getAvailableSlots.handle(new GetAvailableSlotsQuery(offeringId, data)));
        return VIEW_HORARIOS;
    }

    private static String enderecoRemoto(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
