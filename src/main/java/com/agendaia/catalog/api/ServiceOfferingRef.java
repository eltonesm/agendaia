package com.agendaia.catalog.api;

import com.agendaia.shared.Money;
import java.util.UUID;

/**
 * Projeção de {@code ServiceOffering} exportada para outros contextos.
 *
 * <p>Igual em forma a {@code ServiceOfferingView} (uso interno de
 * {@code catalog}), mas um tipo diferente — mesmo raciocínio de
 * {@code organization.api.ProfessionalRef}: um pertence à {@code api}, o
 * outro a {@code application.port.in}, e podem divergir livremente no
 * futuro.
 *
 * <p>{@code serviceName}/{@code price} ganhos em pagina-publica-
 * agendamento (TODO-006) — {@code BookAppointmentHandler} precisa dos
 * dois para gravar o retrato de {@code Appointment} (BR-2); a TODO-005
 * (que só calcula disponibilidade) simplesmente não os usa.
 */
public record ServiceOfferingRef(
        UUID id, UUID professionalId, int durationMinutes, int bufferMinutes, String serviceName, Money price) {}
