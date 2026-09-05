package com.agendaia.catalog.api;

import java.util.UUID;

/**
 * Uma oferta ativa de um serviço, já com o nome do profissional e o preço
 * formatado resolvidos — o template da página pública não decide nada
 * (pagina-publica-agendamento, TODO-006).
 */
public record PublicOfferingRef(
        UUID id, UUID professionalId, String professionalName, int durationMinutes, String priceFormatted) {}
