package com.agendaia.billing.adapter.in.web;

import com.agendaia.billing.adapter.in.web.request.ExtendAccessRequest;
import com.agendaia.billing.application.BillingAccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Painel do operador (US-1, US-2, US-5). Não conhece repositório nem
 * entidade: fala com {@link BillingAccountService} e devolve tela. A rota
 * já está protegida por estar sob {@code /operador/**}
 * ({@code OperatorSecurityConfig}), sem checagem extra aqui.
 */
@Controller
public class OperatorPanelController {

    private static final String VIEW = "operador/painel";
    private static final String PRG = "redirect:/operador/painel";

    private final BillingAccountService billingAccountService;

    public OperatorPanelController(BillingAccountService billingAccountService) {
        this.billingAccountService = billingAccountService;
    }

    @GetMapping("/operador/painel")
    public String painel(Model model) {
        model.addAttribute("estabelecimentos", billingAccountService.listForOperator());
        return VIEW;
    }

    @PostMapping("/operador/estabelecimentos/{tenantId}/prazo")
    public String marcarPrazo(
            @PathVariable UUID tenantId,
            @Valid @ModelAttribute("form") ExtendAccessRequest form,
            BindingResult binding,
            Model model) {

        // Erro de formato devolve a MESMA tela com 200, não 400 — e com a
        // lista recarregada, indicando qual estabelecimento falhou.
        if (binding.hasErrors()) {
            model.addAttribute("estabelecimentos", billingAccountService.listForOperator());
            model.addAttribute("erroTenantId", tenantId);
            return VIEW;
        }

        billingAccountService.extendUntil(tenantId, form.accessValidUntil());

        // Post-Redirect-Get: evita reenvio do formulário ao atualizar a
        // página, e o operador já vê o status novo na lista.
        return PRG;
    }
}
