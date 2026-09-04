package com.agendaia.organization.application;

import com.agendaia.organization.api.AvailabilityDirectory;
import com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository;
import com.agendaia.organization.application.port.out.TimeOffRepository;
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TimeRange;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação de {@link AvailabilityDirectory}.
 *
 * <p>Tenant lido do {@link TenantContext}, nunca de argumento (DD-1).
 *
 * <p>{@link #blocksFor} é o único ponto do projeto que converte
 * {@code Instant} (como {@code TimeOff} persiste) para {@code LocalTime}
 * (como {@code scheduling.domain} consome) — a conversão via
 * {@link ZoneId#systemDefault()} acontece aqui, dentro de {@code organization}
 * (que já depende de Spring), nunca em {@code scheduling.domain} (DD-4).
 */
@Service
public class AvailabilityDirectoryHandler implements AvailabilityDirectory {

    private final BusinessOperatingHoursRepository businessOperatingHoursRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final TimeOffRepository timeOffRepository;

    public AvailabilityDirectoryHandler(
            BusinessOperatingHoursRepository businessOperatingHoursRepository,
            WorkScheduleRepository workScheduleRepository,
            TimeOffRepository timeOffRepository) {
        this.businessOperatingHoursRepository = businessOperatingHoursRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.timeOffRepository = timeOffRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeRange> operatingHoursFor(DayOfWeek dayOfWeek) {
        var tenantId = TenantContext.require();

        return businessOperatingHoursRepository
                .findByTenantIdAndDayOfWeekAndActiveTrue(tenantId.value(), dayOfWeek)
                .stream()
                .map(faixa -> new TimeRange(faixa.opensAt(), faixa.closesAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeRange> workScheduleFor(UUID professionalId, DayOfWeek dayOfWeek) {
        var tenantId = TenantContext.require();

        return workScheduleRepository
                .findByTenantIdAndProfessionalIdAndDayOfWeekAndActiveTrue(tenantId.value(), professionalId, dayOfWeek)
                .stream()
                .map(faixa -> new TimeRange(faixa.startsAt(), faixa.endsAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeRange> blocksFor(UUID professionalId, LocalDate date) {
        var tenantId = TenantContext.require();
        var zone = ZoneId.systemDefault();
        var dayStart = date.atStartOfDay(zone).toInstant();
        var dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        return timeOffRepository.findOverlapping(tenantId.value(), professionalId, dayStart, dayEnd).stream()
                .map(bloqueio -> new TimeRange(
                        clipStart(bloqueio.startsAt(), dayStart, zone), clipEnd(bloqueio.endsAt(), dayEnd, zone)))
                .toList();
    }

    /** Recorta o início do bloqueio para 00:00 se ele começou antes do dia consultado. */
    private static LocalTime clipStart(Instant startsAt, Instant dayStart, ZoneId zone) {
        if (!startsAt.isAfter(dayStart)) {
            return LocalTime.MIN;
        }
        return LocalDateTime.ofInstant(startsAt, zone).toLocalTime();
    }

    /** Recorta o fim do bloqueio para o fim do dia se ele termina depois do dia consultado. */
    private static LocalTime clipEnd(Instant endsAt, Instant dayEnd, ZoneId zone) {
        if (!endsAt.isBefore(dayEnd)) {
            return LocalTime.MAX;
        }
        return LocalDateTime.ofInstant(endsAt, zone).toLocalTime();
    }
}
