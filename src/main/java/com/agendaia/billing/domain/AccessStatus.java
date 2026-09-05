package com.agendaia.billing.domain;

/**
 * Status de acesso de um estabelecimento — sempre calculado a partir de
 * {@link BillingAccount#statusOn}, nunca gravado à parte (BR-2).
 */
public enum AccessStatus {

    /** {@code accessValidUntil} nunca foi estendido além de {@code trialEndsAt}. */
    TRIAL,

    /** {@code accessValidUntil} foi estendido além de {@code trialEndsAt} — prova de pagamento marcado. */
    PAID,

    /** Venceu, mas ainda dentro dos 5 dias corridos de carência. */
    GRACE_PERIOD,

    /** Venceu a carência — acesso ao painel administrativo é bloqueado. */
    BLOCKED
}
