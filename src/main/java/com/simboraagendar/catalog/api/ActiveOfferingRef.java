package com.simboraagendar.catalog.api;

import java.util.UUID;

/**
 * Uma oferta ativa do tenant, com nome de profissional e de serviço já
 * resolvidos — usada pelo dropdown único de oferta do painel administrativo
 * (agenda-profissional, TODO-008, DD-10: sem cascata de JavaScript, um único
 * select com "Serviço — Profissional").
 */
public record ActiveOfferingRef(
        UUID id,
        UUID professionalId,
        String professionalName,
        String serviceName,
        int durationMinutes,
        String priceFormatted) {}
