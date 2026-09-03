package com.agendaia.organization.adapter.in.web;

import com.agendaia.organization.adapter.in.web.request.RegisterWorkScheduleRequest;
import com.agendaia.organization.application.command.RegisterWorkScheduleCommand;
import com.agendaia.organization.application.port.in.ListProfessionalsUseCase;
import com.agendaia.organization.application.port.in.ListWorkSchedulesUseCase;
import com.agendaia.organization.application.port.in.RegisterWorkScheduleUseCase;
import com.agendaia.shared.DomainException;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Cadastro e lista de jornada, numa tela só (DD-1).
 *
 * <p>O dropdown de profissional vem de {@link ListProfessionalsUseCase}, já
 * existente desde a TODO-002 — sem caso de uso novo só para popular
 * formulário.
 */
@Controller
public class WorkScheduleController {

    private static final String VIEW = "admin/jornadas";
    private static final String PRG = "redirect:/admin/jornadas";

    private final RegisterWorkScheduleUseCase registerWorkSchedule;
    private final ListWorkSchedulesUseCase listWorkSchedules;
    private final ListProfessionalsUseCase listProfessionals;

    public WorkScheduleController(
            RegisterWorkScheduleUseCase registerWorkSchedule,
            ListWorkSchedulesUseCase listWorkSchedules,
            ListProfessionalsUseCase listProfessionals) {
        this.registerWorkSchedule = registerWorkSchedule;
        this.listWorkSchedules = listWorkSchedules;
        this.listProfessionals = listProfessionals;
    }

    @GetMapping("/admin/jornadas")
    public String listar(Model model) {
        model.addAttribute("form", new RegisterWorkScheduleRequest());
        carregarListas(model);
        return VIEW;
    }

    @PostMapping("/admin/jornadas")
    public String cadastrar(
            @Valid @ModelAttribute("form") RegisterWorkScheduleRequest form, BindingResult binding, Model model) {

        // Erro de formato devolve a MESMA tela com 200, não 400 — e com as
        // listas recarregadas.
        if (binding.hasErrors()) {
            carregarListas(model);
            return VIEW;
        }

        try {
            registerWorkSchedule.register(new RegisterWorkScheduleCommand(
                    form.professionalId(), form.dayOfWeek(), form.startsAt(), form.endsAt()));
        } catch (DomainException e) {
            // Profissional de outro tenant ou faixa sobreposta (BR-3/BR-8)
            // vira erro NO CAMPO que os causou, nunca 500.
            binding.rejectValue(e.field(), "invalido", e.getMessage());
            carregarListas(model);
            return VIEW;
        }

        // Post-Redirect-Get: evita reenvio do formulário ao atualizar a
        // página, e o dono já vê a faixa na lista.
        return PRG;
    }

    private void carregarListas(Model model) {
        model.addAttribute("faixas", listWorkSchedules.list());
        model.addAttribute("profissionais", listProfessionals.list());
        model.addAttribute("diasDaSemana", DayOfWeek.values());
    }
}
