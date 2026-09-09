package com.agendaia.billing.adapter.in.web;

import com.agendaia.billing.adapter.in.web.request.ExtendAccessRequest;
import com.agendaia.billing.application.BillingAccountService;
import com.agendaia.billing.application.EstablishmentView;
import com.agendaia.billing.domain.AccessStatus;
import jakarta.validation.Valid;
import java.util.List;
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
        carregarPainel(model);
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
            carregarPainel(model);
            model.addAttribute("erroTenantId", tenantId);
            return VIEW;
        }

        billingAccountService.extendUntil(tenantId, form.accessValidUntil());

        // Post-Redirect-Get: evita reenvio do formulário ao atualizar a
        // página, e o operador já vê o status novo na lista.
        return PRG;
    }

    /**
     * Contadores por status são só uma leitura da mesma lista já buscada —
     * nenhuma consulta a mais ao banco (importante: {@link #painel} roda a
     * cada carregamento da tela).
     */
    private void carregarPainel(Model model) {
        var estabelecimentos = billingAccountService.listForOperator();
        model.addAttribute("estabelecimentos", estabelecimentos);
        model.addAttribute("totalEmpresas", estabelecimentos.size());
        model.addAttribute("totalPagas", contarPorStatus(estabelecimentos, AccessStatus.PAID));
        model.addAttribute("totalCarencia", contarPorStatus(estabelecimentos, AccessStatus.GRACE_PERIOD));
        model.addAttribute("totalBloqueadas", contarPorStatus(estabelecimentos, AccessStatus.BLOCKED));
    }

    private static long contarPorStatus(List<EstablishmentView> estabelecimentos, AccessStatus status) {
        return estabelecimentos.stream().filter(e -> e.status() == status).count();
    }
}
