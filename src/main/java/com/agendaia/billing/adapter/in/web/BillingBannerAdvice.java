package com.agendaia.billing.adapter.in.web;

import com.agendaia.billing.domain.AccessStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Aviso de carência e contato de WhatsApp em toda página do painel
 * administrativo (US-3/US-6, BR-4/BR-5). Mora em {@code billing} pelo
 * mesmo motivo de {@link AccessGuardFilter} (DD-4 da spec técnica) — não é
 * o {@code platform.web.LayoutAdvice} que decide isso, embora o mecanismo
 * (um {@code @ModelAttribute} lido pelo layout compartilhado) seja o
 * mesmo.
 */
@ControllerAdvice
public class BillingBannerAdvice {

    private final String operadorWhatsapp;

    public BillingBannerAdvice(@Value("${agendaia.operador.whatsapp}") String operadorWhatsapp) {
        this.operadorWhatsapp = operadorWhatsapp;
    }

    /**
     * Nulo quando não há carência — nunca consulta o banco: reaproveita o
     * atributo de requisição que {@link AccessGuardFilter} já calculou.
     */
    @ModelAttribute("carenciaAte")
    public LocalDate carenciaAte(HttpServletRequest request) {
        var snapshot = AccessGuardFilter.snapshotFrom(request);
        if (snapshot == null || snapshot.status() != AccessStatus.GRACE_PERIOD) {
            return null;
        }
        return snapshot.graceEndsAt();
    }

    /** Número em E.164 sem "+" (ex.: {@code 5511999999999}), pronto para {@code wa.me/}. */
    @ModelAttribute("operadorWhatsapp")
    public String operadorWhatsapp() {
        return operadorWhatsapp;
    }
}
