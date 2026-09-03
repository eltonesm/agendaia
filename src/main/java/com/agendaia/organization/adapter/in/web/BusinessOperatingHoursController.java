package com.agendaia.organization.adapter.in.web;

import com.agendaia.organization.adapter.in.web.request.RegisterBusinessOperatingHoursRequest;
import com.agendaia.organization.application.command.RegisterBusinessOperatingHoursCommand;
import com.agendaia.organization.application.port.in.ListBusinessOperatingHoursUseCase;
import com.agendaia.organization.application.port.in.RegisterBusinessOperatingHoursUseCase;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Cadastro e lista de horário de funcionamento, numa tela só (DD-1).
 *
 * <p>Não conhece repositório nem entidade: fala com os dois casos de uso e
 * devolve tela. Nenhum dos dois recebe tenant como argumento (DD-1) — a rota
 * já está protegida por estar sob {@code /admin/**}, sem mudança em
 * {@code SecurityConfig}.
 */
@Controller
public class BusinessOperatingHoursController {

    private static final String VIEW = "admin/horario-funcionamento";
    private static final String PRG = "redirect:/admin/horario-funcionamento";

    private final RegisterBusinessOperatingHoursUseCase registerBusinessOperatingHours;
    private final ListBusinessOperatingHoursUseCase listBusinessOperatingHours;

    public BusinessOperatingHoursController(
            RegisterBusinessOperatingHoursUseCase registerBusinessOperatingHours,
            ListBusinessOperatingHoursUseCase listBusinessOperatingHours) {
        this.registerBusinessOperatingHours = registerBusinessOperatingHours;
        this.listBusinessOperatingHours = listBusinessOperatingHours;
    }

    @GetMapping("/admin/horario-funcionamento")
    public String listar(Model model) {
        model.addAttribute("form", new RegisterBusinessOperatingHoursRequest());
        model.addAttribute("faixas", listBusinessOperatingHours.list());
        model.addAttribute("diasDaSemana", DayOfWeek.values());
        return VIEW;
    }

    @PostMapping("/admin/horario-funcionamento")
    public String cadastrar(
            @Valid @ModelAttribute("form") RegisterBusinessOperatingHoursRequest form,
            BindingResult binding,
            Model model) {

        // Erro de formato devolve a MESMA tela com 200, não 400 — e com a
        // lista recarregada.
        if (binding.hasErrors()) {
            model.addAttribute("faixas", listBusinessOperatingHours.list());
            model.addAttribute("diasDaSemana", DayOfWeek.values());
            return VIEW;
        }

        registerBusinessOperatingHours.register(
                new RegisterBusinessOperatingHoursCommand(form.dayOfWeek(), form.opensAt(), form.closesAt()));

        // Post-Redirect-Get: evita reenvio do formulário ao atualizar a
        // página, e o dono já vê a faixa na lista.
        return PRG;
    }
}
