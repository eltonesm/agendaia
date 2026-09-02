package com.agendaia.catalog.application.port.in;

import com.agendaia.catalog.application.command.RegisterServiceOfferingCommand;

/**
 * Cadastra uma oferta do estabelecimento da sessão.
 *
 * <p>Valida, via {@code organization.api.ProfessionalDirectory}, que o
 * profissional recebido pertence ao tenant da sessão (DD-1, BR-8) — não há
 * chave estrangeira que garanta isso no banco (DD-2).
 */
public interface RegisterServiceOfferingUseCase {

    RegisteredServiceOffering register(RegisterServiceOfferingCommand command);
}
