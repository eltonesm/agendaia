package com.agendaia.organization.adapter.in.web;

import com.agendaia.organization.adapter.in.web.request.RegisterProfessionalRequest;
import com.agendaia.organization.application.command.RegisterProfessionalCommand;
import com.agendaia.organization.application.port.in.ListProfessionalsUseCase;
import com.agendaia.organization.application.port.in.RegisterProfessionalUseCase;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Cadastro e lista de profissionais, numa tela só (DD-2).
 *
 * <p>Não conhece repositório nem entidade: fala com os dois casos de uso e
 * devolve tela. Nenhum dos dois recebe tenant como argumento (DD-1) — a rota
 * já está protegida por estar sob {@code /admin/**}, sem mudança em
 * {@code SecurityConfig}.
 */
@Controller
public class ProfessionalController {

    private static final String VIEW = "admin/profissionais";
    private static final String PRG = "redirect:/admin/profissionais";

    private final RegisterProfessionalUseCase registerProfessional;
    private final ListProfessionalsUseCase listProfessionals;

    public ProfessionalController(
            RegisterProfessionalUseCase registerProfessional,
            ListProfessionalsUseCase listProfessionals) {
        this.registerProfessional = registerProfessional;
        this.listProfessionals = listProfessionals;
    }

    @GetMapping("/admin/profissionais")
    public String listar(Model model) {
        model.addAttribute("form", new RegisterProfessionalRequest());
        model.addAttribute("profissionais", listProfessionals.list());
        return VIEW;
    }

    @PostMapping("/admin/profissionais")
    public String cadastrar(
            @Valid @ModelAttribute("form") RegisterProfessionalRequest form,
            BindingResult binding,
            Model model) {

        // Erro de formato devolve a MESMA tela com 200, não 400 — e com a
        // lista recarregada: sem isso o dono perderia de vista quem já tinha
        // cadastrado ao errar o próximo nome.
        if (binding.hasErrors()) {
            model.addAttribute("profissionais", listProfessionals.list());
            return VIEW;
        }

        registerProfessional.register(new RegisterProfessionalCommand(form.name()));

        // Post-Redirect-Get: evita reenvio do formulário ao atualizar a
        // página, e o dono já vê o profissional na lista (DD-2).
        return PRG;
    }
}
