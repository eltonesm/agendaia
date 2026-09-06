package com.agendaia.scheduling.adapter.out.persistence;

import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.exception.SlotUnavailableException;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.TimeRange;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Implementação de {@link AppointmentRepository} — o único ponto do
 * projeto que traduz a exclusion constraint do ADR 0005 (DD-6 da spec
 * técnica de pagina-publica-agendamento).
 */
@Component
public class AppointmentPersistenceAdapter implements AppointmentRepository {

    /** BR-9/BR-6: agendamento CONFIRMED conta para o teto tanto quanto SCHEDULED. */
    private static final List<AppointmentStatus> ATIVOS_PARA_TETO =
            List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);

    private final AppointmentJpaRepository appointmentJpaRepository;

    public AppointmentPersistenceAdapter(AppointmentJpaRepository appointmentJpaRepository) {
        this.appointmentJpaRepository = appointmentJpaRepository;
    }

    @Override
    public Appointment save(Appointment appointment) {
        try {
            var salvo = appointmentJpaRepository.saveAndFlush(AppointmentMapper.toEntity(appointment));
            return AppointmentMapper.toDomain(salvo);
        } catch (DataIntegrityViolationException e) {
            // A exclusion constraint (appointment_no_overlap) é quem decide de
            // verdade — este catch só traduz a violação em erro tratado (US-6).
            throw new SlotUnavailableException();
        }
    }

    @Override
    public Optional<Appointment> findByTenantIdAndId(TenantId tenantId, UUID id) {
        return appointmentJpaRepository.findByTenantIdAndId(tenantId.value(), id).map(AppointmentMapper::toDomain);
    }

    @Override
    public void updateStatus(TenantId tenantId, UUID id, AppointmentStatus status, Instant agora) {
        appointmentJpaRepository.updateStatus(tenantId.value(), id, status, agora);
    }

    @Override
    public long countFutureActive(TenantId tenantId, UUID customerId, Instant agora) {
        return appointmentJpaRepository.countByTenantIdAndCustomerIdAndStatusInAndStartsAtAfter(
                tenantId.value(), customerId, ATIVOS_PARA_TETO, agora);
    }

    /** Mesma técnica de recorte de {@code AvailabilityDirectoryHandler#blocksFor} (organization). */
    @Override
    public List<TimeRange> findOccupiedRanges(TenantId tenantId, UUID professionalId, LocalDate date) {
        var zone = ZoneId.systemDefault();
        var dayStart = date.atStartOfDay(zone).toInstant();
        var dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        return appointmentJpaRepository.findOverlapping(tenantId.value(), professionalId, dayStart, dayEnd).stream()
                .map(agendamento -> new TimeRange(
                        clipStart(agendamento.startsAt(), dayStart, zone),
                        clipEnd(agendamento.endsAt(), dayEnd, zone)))
                .toList();
    }

    private static LocalTime clipStart(Instant startsAt, Instant dayStart, ZoneId zone) {
        if (!startsAt.isAfter(dayStart)) {
            return LocalTime.MIN;
        }
        return LocalDateTime.ofInstant(startsAt, zone).toLocalTime();
    }

    private static LocalTime clipEnd(Instant endsAt, Instant dayEnd, ZoneId zone) {
        if (!endsAt.isBefore(dayEnd)) {
            return LocalTime.MAX;
        }
        return LocalDateTime.ofInstant(endsAt, zone).toLocalTime();
    }
}
