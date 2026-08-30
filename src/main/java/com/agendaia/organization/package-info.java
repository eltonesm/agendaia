/**
 * Contexto delimitado Organization — quem somos e quando trabalhamos.
 *
 * <p>Dono de {@code Business} (que <em>é</em> o tenant), {@code User},
 * {@code Professional}, {@code BusinessOperatingHours}, {@code WorkSchedule} e
 * {@code TimeOff}.
 *
 * <p>Subdomínio de suporte: a entidade JPA é o modelo, sem classe espelho nem
 * mapper (ADR 0002). Identidade vive aqui, e não num contexto IAM separado,
 * porque tenant e empresa são a mesma entidade no MVP (ADR 0003).
 *
 * <p>Outros contextos só podem importar {@code com.agendaia.organization.api}.
 */
@ApplicationModule(displayName = "Organization")
package com.agendaia.organization;

import org.springframework.modulith.ApplicationModule;
