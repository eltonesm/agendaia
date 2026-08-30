/**
 * Contexto delimitado Customer — quem é atendido pelo estabelecimento.
 *
 * <p>Dono de {@code Customer}: nome, telefone e consentimento. Sem login no MVP;
 * o cliente é localizado ou criado a partir do formulário público.
 *
 * <p>Deliberadamente magro. O <em>histórico</em> de um cliente não é entidade
 * daqui: é consulta em {@code scheduling} filtrada por {@code customerId}. Criar
 * uma tabela de histórico aqui seria uma segunda fonte da verdade sobre
 * agendamentos.
 *
 * <p>Não confundir com quem paga a mensalidade do AgendaIA — esse é o
 * <em>estabelecimento</em>, {@code Business}, no contexto Organization.
 *
 * <p>Outros contextos só podem importar {@code com.agendaia.customer.api}.
 */
@ApplicationModule(displayName = "Customer")
package com.agendaia.customer;

import org.springframework.modulith.ApplicationModule;
