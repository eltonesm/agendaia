package com.agendaia.catalog.application.port.in;

import java.util.UUID;

/**
 * Uma linha da lista de ofertas — já com nomes resolvidos (não ids crus), o
 * template não precisa de lógica nenhuma.
 */
public record ServiceOfferingView(
        UUID id, String serviceName, String professionalName, int durationMinutes, String priceFormatted) {}
