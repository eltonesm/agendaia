package com.agendaia.organization.application.port.out;

import com.agendaia.organization.domain.TimeOff;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Porta de saída do bloqueio.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface TimeOffRepository extends JpaRepository<TimeOff, UUID> {

    /** Usado na listagem, mais recente primeiro. */
    List<TimeOff> findByTenantIdAndActiveTrueOrderByStartsAtDesc(UUID tenantId);

    /**
     * Bloqueios do profissional específico OU gerais (professionalId nulo)
     * que se sobrepõem ao intervalo {@code [dayStart, dayEnd)}. Usado por
     * {@code AvailabilityDirectory.blocksFor} (consultar-horarios-disponiveis,
     * BR-2, DD-5).
     *
     * <p>{@code @Query} JPQL, não derived method: a condição "profissional
     * específico OU bloqueio geral" não tem forma limpa como nome de método
     * Spring Data. Totalmente parametrizada — mesma garantia de segurança de
     * um derived method (DD-5).
     */
    @Query("""
            select t from TimeOff t
            where t.tenantId = :tenantId
              and t.active = true
              and t.startsAt < :dayEnd
              and t.endsAt > :dayStart
              and (t.professionalId = :professionalId or t.professionalId is null)
            """)
    List<TimeOff> findOverlapping(
            @Param("tenantId") UUID tenantId,
            @Param("professionalId") UUID professionalId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd);
}
