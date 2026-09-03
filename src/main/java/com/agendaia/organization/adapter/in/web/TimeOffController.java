package com.agendaia.organization.adapter.in.web;

import com.agendaia.organization.adapter.in.web.request.RegisterTimeOffRequest;
import com.agendaia.organization.application.command.RegisterTimeOffCommand;
import com.agendaia.organization.application.port.in.ListProfessionalsUseCase;
import com.agendaia.organization.application.port.in.ListTimeOffUseCase;
import com.agendaia.organization.application.port.in.RegisterTimeOffUseCase;
import com.agendaia.shared.DomainException;
import jakarta.validation.Valid;
import java.time.ZoneId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Cadastro e lista de bloqueios, numa tela só (DD-1).
 *
 * <p>O dropdown de profissional vem de {@link ListProfessionalsUseCase}, já
 * existente desde a TODO-002, com uma opção extra de "estabelecimento
 * inteiro" que o dropdown de {@code /admin/jornadas} não tem.
 */
@Controller
public class TimeOffController {

    private static final String VIEW = "admin/bloqueios";
    private static final String PRG = "redirect:/admin/bloqueios";

    private final RegisterTimeOffUseCase registerTimeOff;
    private final ListTimeOffUseCase listTimeOff;
    private final ListProfessionalsUseCase listProfessionals;

    public TimeOffController(
            RegisterTimeOffUseCase registerTimeOff,
            ListTimeOffUseCase listTimeOff,
            ListProfessionalsUseCase listProfessionals) {
        this.registerTimeOff = registerTimeOff;
        this.listTimeOff = listTimeOff;
        this.listProfessionals = listProfessionals;
    }

    @GetMapping("/admin/bloqueios")
    public String listar(Model model) {
        model.addAttribute("form", new RegisterTimeOffRequest());
        carregarListas(model);
        return VIEW;
    }

    @PostMapping("/admin/bloqueios")
    public String cadastrar(@Valid @ModelAttribute("form") RegisterTimeOffRequest form, BindingResult binding, Model model) {

        // Erro de formato devolve a MESMA tela com 200, não 400 — e com as
        // listas recarregadas.
        if (binding.hasErrors()) {
            carregarListas(model);
            return VIEW;
        }

        try {
            var zona = ZoneId.systemDefault();
            registerTimeOff.register(new RegisterTimeOffCommand(
                    form.professionalId(),
                    form.startsAt().atZone(zona).toInstant(),
                    form.endsAt().atZone(zona).toInstant(),
                    form.reason()));
        } catch (DomainException e) {
            // Profissional de outro tenant (BR-8) vira erro NO CAMPO,
            // nunca 500.
            binding.rejectValue(e.field(), "invalido", e.getMessage());
            carregarListas(model);
            return VIEW;
        }

        // Post-Redirect-Get: evita reenvio do formulário ao atualizar a
        // página, e o dono já vê o bloqueio na lista.
        return PRG;
    }

    private void carregarListas(Model model) {
        model.addAttribute("bloqueios", listTimeOff.list());
        model.addAttribute("profissionais", listProfessionals.list());
    }
}
