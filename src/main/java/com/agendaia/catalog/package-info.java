/**
 * Contexto delimitado Catalog — o que o estabelecimento vende.
 *
 * <p>Dono de {@code Service} (o conceito: "Corte de cabelo", sem preço nem
 * duração) e de {@code ServiceOffering} (o serviço executado por um profissional
 * específico, com duração, preço e intervalo próprios).
 *
 * <p>{@code ServiceOffering} guarda {@code professionalId} como UUID solto: a
 * referência atravessa o contexto por identificador, nunca por chave estrangeira
 * entre schemas.
 *
 * <p>Outros contextos só podem importar {@code com.agendaia.catalog.api}.
 */
@ApplicationModule(
        displayName = "Catalog",
        allowedDependencies = {"organization :: api"})
package com.agendaia.catalog;

import org.springframework.modulith.ApplicationModule;
