package com.agendaia.organization.application.port.in;

import java.util.UUID;

/**
 * Uma linha da lista de profissionais.
 *
 * <p>Sem {@code active}: a listagem nunca mostra inativo (não há tela de
 * desativação nesta feature), então o campo não teria uso na tela.
 */
public record ProfessionalView(UUID id, String name) {}
