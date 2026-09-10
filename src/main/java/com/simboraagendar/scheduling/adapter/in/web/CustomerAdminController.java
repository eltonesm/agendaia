package com.simboraagendar.scheduling.adapter.in.web;

import com.simboraagendar.scheduling.application.port.in.CustomerActivityDetailUseCase;
import com.simboraagendar.scheduling.application.port.in.ListCustomerActivityUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lista e detalhe de clientes do dono (gestao-de-clientes: IDEA-018,
 * IDEA-019, IDEA-006).
 *
 * <p>Mora em {@code scheduling}, não em {@code customer} nem {@code
 * organization} (DD-1 da spec técnica) — reaproveita a dependência de
 * {@code customer.api} que {@code scheduling} já declara desde a
 * TODO-008/TODO-110, evitando o ciclo que a direção contrária fecharia.
 *
 * <p>Sem configuração nova em {@code SecurityConfig}: {@code /admin/**} já
 * cai em {@code anyRequest().hasRole("OWNER")} por omissão. O tenant vem da
 * sessão autenticada, via {@code TenantContext}, resolvido antes deste
 * controller rodar.
 */
@Controller
@RequestMapping("/admin/clientes")
public class CustomerAdminController {

    private static final String VIEW_LISTA = "admin/clientes";
    private static final String VIEW_DETALHE = "admin/cliente-detalhe";
    private static final int TAMANHO_PAGINA_PADRAO = 20;

    private final ListCustomerActivityUseCase listCustomerActivity;
    private final CustomerActivityDetailUseCase customerActivityDetail;

    public CustomerAdminController(
            ListCustomerActivityUseCase listCustomerActivity, CustomerActivityDetailUseCase customerActivityDetail) {
        this.listCustomerActivity = listCustomerActivity;
        this.customerActivityDetail = customerActivityDetail;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "0") int page, Model model) {
        var pagina = listCustomerActivity.list(page, TAMANHO_PAGINA_PADRAO);
        model.addAttribute("pagina", pagina);
        return VIEW_LISTA;
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable UUID id, Model model) {
        var detalhe = customerActivityDetail
                .detail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("cliente", detalhe);
        return VIEW_DETALHE;
    }
}
