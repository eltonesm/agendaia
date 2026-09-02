package com.agendaia.catalog.adapter.in.web;

import com.agendaia.catalog.adapter.in.web.request.RegisterServiceRequest;
import com.agendaia.catalog.application.command.RegisterServiceCommand;
import com.agendaia.catalog.application.port.in.ListServicesUseCase;
import com.agendaia.catalog.application.port.in.RegisterServiceUseCase;
import com.agendaia.shared.DomainException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Cadastro e lista de serviços, numa tela só (DD-4).
 *
 * <p>Não conhece repositório nem entidade: fala com os dois casos de uso e
 * devolve tela. Nenhum dos dois recebe tenant como argumento (DD-1) — a rota
 * já está protegida por estar sob {@code /admin/**}, sem mudança em
 * {@code SecurityConfig}.
 */
@Controller
public class ServiceController {

    private static final String VIEW = "admin/servicos";
    private static final String PRG = "redirect:/admin/servicos";

    private final RegisterServiceUseCase registerService;
    private final ListServicesUseCase listServices;

    public ServiceController(RegisterServiceUseCase registerService, ListServicesUseCase listServices) {
        this.registerService = registerService;
        this.listServices = listServices;
    }

    @GetMapping("/admin/servicos")
    public String listar(Model model) {
        model.addAttribute("form", new RegisterServiceRequest());
        model.addAttribute("servicos", listServices.list());
        return VIEW;
    }

    @PostMapping("/admin/servicos")
    public String cadastrar(
            @Valid @ModelAttribute("form") RegisterServiceRequest form, BindingResult binding, Model model) {

        // Erro de formato devolve a MESMA tela com 200, não 400 — e com a
        // lista recarregada: sem isso o dono perderia de vista quem já tinha
        // cadastrado ao errar o próximo nome.
        if (binding.hasErrors()) {
            model.addAttribute("servicos", listServices.list());
            return VIEW;
        }

        try {
            registerService.register(new RegisterServiceCommand(form.name(), form.description()));
        } catch (DomainException e) {
            // Nome duplicado (BR-1) vira erro NO CAMPO que o causou,
            // preservando o resto do preenchimento.
            binding.rejectValue(e.field(), "indisponivel", e.getMessage());
            model.addAttribute("servicos", listServices.list());
            return VIEW;
        }

        // Post-Redirect-Get: evita reenvio do formulário ao atualizar a
        // página, e o dono já vê o serviço na lista (DD-4).
        return PRG;
    }
}
