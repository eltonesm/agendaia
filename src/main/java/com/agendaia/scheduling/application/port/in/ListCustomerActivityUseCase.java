package com.agendaia.scheduling.application.port.in;

/** Lista paginada de clientes com atividade (gestao-de-clientes, US-1). */
public interface ListCustomerActivityUseCase {

    PagedResult<CustomerListEntry> list(int page, int size);
}
