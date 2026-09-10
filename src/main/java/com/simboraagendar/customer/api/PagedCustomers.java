package com.simboraagendar.customer.api;

import java.util.List;

/** Página de clientes do tenant (gestao-de-clientes), ordenada por nome. */
public record PagedCustomers(List<CustomerRef> items, int page, int size, long totalElements) {}
