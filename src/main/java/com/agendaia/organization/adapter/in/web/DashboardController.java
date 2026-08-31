package com.agendaia.organization.adapter.in.web;

import com.agendaia.organization.application.port.in.ViewDashboardUseCase;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Painel do estabelecimento.
 *
 * <p>Mínimo de propósito: nesta feature ele existe para provar que a sessão
 * funciona e para mostrar ao dono o link que ele acabou de criar. As telas de
 * verdade chegam nas features seguintes.
 */
@Controller
public class DashboardController {

    private final ViewDashboardUseCase viewDashboard;

    public DashboardController(ViewDashboardUseCase viewDashboard) {
        this.viewDashboard = viewDashboard;
    }

    @GetMapping("/admin/dashboard")
    public String painel(Model model) {
        // Sem parâmetro de tenant na assinatura: quem decide de qual
        // estabelecimento é este painel é a sessão, não a URL.
        model.addAttribute("painel", viewDashboard.current());
        return "admin/dashboard";
    }
}
