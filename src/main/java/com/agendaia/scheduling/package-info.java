/**
 * Contexto delimitado Scheduling — o core domain do AgendaIA.
 *
 * <p>Dono de {@code Appointment} e do cálculo de disponibilidade: horário da
 * empresa ∩ jornada do profissional − bloqueios − agendamentos, filtrado por
 * quem comporta a duração e o intervalo da oferta.
 *
 * <p>Único contexto com o regime completo de Clean Architecture (ADR 0002):
 * {@code domain} é Java puro, a entidade JPA é classe separada, e o mapeamento
 * entre as duas é explícito. É aqui que mora a regra que precisa ser testável em
 * milissegundos, sem subir Spring nem banco.
 *
 * <p>Outros contextos só podem importar {@code com.agendaia.scheduling.api}.
 *
 * <p>{@code allowedDependencies} declarado explicitamente desde o primeiro
 * commit real deste contexto (consultar-horarios-disponiveis, DD-1): uma vez
 * declarada, a lista vira whitelist mesmo para módulo {@code Type.OPEN} —
 * {@code shared} e {@code platform} precisam estar aqui, ou
 * {@code ModuleStructureTest} falha (mesmo gotcha da TODO-003, documentado em
 * {@code PATTERNS.md}).
 */
@ApplicationModule(
        displayName = "Scheduling (core)",
        allowedDependencies = {"organization :: api", "catalog :: api", "shared", "platform"})
package com.agendaia.scheduling;

import org.springframework.modulith.ApplicationModule;
