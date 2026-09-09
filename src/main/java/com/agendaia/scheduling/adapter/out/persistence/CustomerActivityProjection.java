package com.agendaia.scheduling.adapter.out.persistence;

import java.util.UUID;

/**
 * Projeção Spring Data para {@link AppointmentJpaRepository#findActivityByCustomerIds}
 * — os aliases da consulta JPQL casam com os nomes destes métodos.
 */
interface CustomerActivityProjection {
    UUID getCustomerId();

    long getVisitCount();

    long getTotalCents();

    long getOwedCents();
}
