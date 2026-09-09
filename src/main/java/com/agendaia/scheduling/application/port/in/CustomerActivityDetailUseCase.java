package com.agendaia.scheduling.application.port.in;

import java.util.Optional;
import java.util.UUID;

/** Detalhe do cliente (gestao-de-clientes, US-2) — vazio se pertencer a outro tenant ou não existir. */
public interface CustomerActivityDetailUseCase {

    Optional<CustomerActivityDetail> detail(UUID customerId);
}
