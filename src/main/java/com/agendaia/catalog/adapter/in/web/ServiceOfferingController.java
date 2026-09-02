package com.agendaia.catalog.adapter.in.web;

import com.agendaia.catalog.adapter.in.web.request.RegisterServiceOfferingRequest;
import com.agendaia.catalog.application.command.RegisterServiceOfferingCommand;
import com.agendaia.catalog.application.port.in.ListServiceOfferingsUseCase;
import com.agendaia.catalog.application.port.in.ListServicesUseCase;
import com.agendaia.catalog.application.port.in.RegisterServiceOfferingUseCase;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.shared.DomainException;
import com.agendaia.shared.Money;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Cadastro e lista de ofertas, numa tela só (DD-4).
 *
 * <p>Os dropdowns de serviço e profissional vêm dos mesmos ports de leitura
 * que já existem — {@link ListServicesUseCase} e
 * {@link ProfessionalDirectory#listActive()} — sem caso de uso novo só para
 * isso. Nenhum dos casos de uso de escrita recebe tenant como argumento
 * (DD-1).
 */
@Controller
public class ServiceOfferingController {

    private static final String VIEW = "admin/ofertas";
    private static final String PRG = "redirect:/admin/ofertas";

    private final RegisterServiceOfferingUseCase registerServiceOffering;
    private final ListServiceOfferingsUseCase listServiceOfferings;
    private final ListServicesUseCase listServices;
    private final ProfessionalDirectory professionalDirectory;

    public ServiceOfferingController(
            RegisterServiceOfferingUseCase registerServiceOffering,
            ListServiceOfferingsUseCase listServiceOfferings,
            ListServicesUseCase listServices,
            ProfessionalDirectory professionalDirectory) {
        this.registerServiceOffering = registerServiceOffering;
        this.listServiceOfferings = listServiceOfferings;
        this.listServices = listServices;
        this.professionalDirectory = professionalDirectory;
    }

    @GetMapping("/admin/ofertas")
    public String listar(Model model) {
        model.addAttribute("form", new RegisterServiceOfferingRequest());
        carregarListas(model);
        return VIEW;
    }

    @PostMapping("/admin/ofertas")
    public String cadastrar(
            @Valid @ModelAttribute("form") RegisterServiceOfferingRequest form, BindingResult binding, Model model) {

        // Erro de formato devolve a MESMA tela com 200, não 400 — e com as
        // listas recarregadas.
        if (binding.hasErrors()) {
            carregarListas(model);
            return VIEW;
        }

        try {
            registerServiceOffering.register(new RegisterServiceOfferingCommand(
                    form.serviceId(),
                    form.professionalId(),
                    form.durationMinutes(),
                    Money.reais(form.price()),
                    form.bufferMinutes()));
        } catch (DomainException e) {
            // Profissional de outro tenant, serviço inexistente ou oferta
            // duplicada viram erro NO CAMPO que os causou (BR-7, BR-8),
            // nunca 500.
            binding.rejectValue(e.field(), "invalido", e.getMessage());
            carregarListas(model);
            return VIEW;
        }

        // Post-Redirect-Get: evita reenvio do formulário ao atualizar a
        // página, e o dono já vê a oferta na lista (DD-4).
        return PRG;
    }

    private void carregarListas(Model model) {
        model.addAttribute("ofertas", listServiceOfferings.list());
        model.addAttribute("servicos", listServices.list());
        model.addAttribute("profissionais", professionalDirectory.listActive());
    }
}
