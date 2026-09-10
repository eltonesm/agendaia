/**
 * Contrato público de {@code catalog} — o único pacote que outros contextos
 * podem importar (ADR 0010).
 *
 * <p>Primeiro contrato de {@code catalog}: {@link com.simboraagendar.catalog.api.ServiceOfferingDirectory#find(java.util.UUID)},
 * usado por {@code scheduling} para resolver profissional, duração e
 * intervalo a partir do id de uma oferta (consultar-horarios-disponiveis,
 * TODO-005).
 *
 * <p>{@link com.simboraagendar.catalog.api.ServiceDirectory} e
 * {@link com.simboraagendar.catalog.api.ServiceOfferingDirectory#listActiveByService(java.util.UUID)}:
 * listagem em lote para a página pública montar o catálogo do
 * estabelecimento (pagina-publica-agendamento, TODO-006, DD-1 da spec
 * técnica) — sempre resolvendo o nome do profissional uma vez só, nunca em
 * laço por oferta.
 */
@NamedInterface("api")
package com.simboraagendar.catalog.api;

import org.springframework.modulith.NamedInterface;
