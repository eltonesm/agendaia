package com.agendaia.catalog.application.port.in;

import java.util.UUID;

/**
 * Uma linha da lista de serviços — também usada para popular o dropdown de
 * serviço na tela de cadastro de oferta.
 */
public record ServiceView(UUID id, String name) {}
