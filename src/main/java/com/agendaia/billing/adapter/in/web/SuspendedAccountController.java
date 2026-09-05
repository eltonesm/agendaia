package com.agendaia.billing.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela de bloqueio (BR-4/US-4) — para onde {@link AccessGuardFilter}
 * redireciona quando o estabelecimento venceu a carência. Sem lógica: o
 * layout compartilhado já mostra o link de WhatsApp em qualquer página do
 * admin (BR-5), inclusive esta.
 */
@Controller
public class SuspendedAccountController {

    @GetMapping("/admin/conta-suspensa")
    public String contaSuspensa() {
        return "admin/conta-suspensa";
    }
}
