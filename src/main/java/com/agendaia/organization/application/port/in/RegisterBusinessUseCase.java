package com.agendaia.organization.application.port.in;

import com.agendaia.organization.application.command.RegisterBusinessCommand;

/**
 * Cria um estabelecimento e o usuário dono.
 *
 * <p>Interface separada da implementação por convenção do time: comunicação
 * entre camadas passa por interface. A implementação usa o sufixo
 * {@code Handler} — nunca {@code Impl}, que seria sintoma de não existir um
 * segundo conceito.
 *
 * <p><strong>Não autentica.</strong> Autenticar a sessão é responsabilidade do
 * adapter web: envolve requisição, resposta e cookie, que não têm o que fazer
 * numa camada que não sabe se existe HTTP. O caso de uso cria e devolve o que a
 * camada web precisa para autenticar.
 */
public interface RegisterBusinessUseCase {

    /**
     * @throws com.agendaia.organization.domain.exception.SlugUnavailableException
     *     se o link estiver em uso ou for reservado
     * @throws com.agendaia.organization.domain.exception.EmailAlreadyUsedException
     *     se o e-mail já tiver conta
     */
    RegisteredBusiness register(RegisterBusinessCommand command);
}
