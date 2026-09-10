package com.simboraagendar.scheduling.adapter.out.persistence;

import com.simboraagendar.scheduling.application.port.out.AppointmentRepository;
import com.simboraagendar.scheduling.application.port.out.CustomerActivity;
import com.simboraagendar.scheduling.domain.Appointment;
import com.simboraagendar.scheduling.domain.AppointmentStatus;
import com.simboraagendar.scheduling.domain.PaymentStatus;
import com.simboraagendar.scheduling.domain.exception.SlotUnavailableException;
import com.simboraagendar.shared.Money;
import com.simboraagendar.shared.TenantId;
import com.simboraagendar.shared.TimeRange;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    @Override
    public List<Appointment> findByTenantIdAndProfessionalIdAndDate(
            TenantId tenantId, UUID professionalId, LocalDate date) {
        var zone = ZoneId.systemDefault();
        var dayStart = date.atStartOfDay(zone).toInstant();
        var dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        return appointmentJpaRepository
                .findByProfessionalAndDay(tenantId.value(), professionalId, dayStart, dayEnd)
                .stream()
                .map(AppointmentMapper::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findByTenantIdAndDate(TenantId tenantId, LocalDate date) {
        var zone = ZoneId.systemDefault();
        var dayStart = date.atStartOfDay(zone).toInstant();
        var dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        return appointmentJpaRepository.findByTenantAndDay(tenantId.value(), dayStart, dayEnd).stream()
                .map(AppointmentMapper::toDomain)
                .toList();
    }

    @Override
    public void updatePaymentStatus(TenantId tenantId, UUID id, PaymentStatus paymentStatus, Instant agora) {
        appointmentJpaRepository.updatePaymentStatus(tenantId.value(), id, paymentStatus, agora);
    }

    @Override
    public List<Appointment> findCompletedByTenantIdAndCustomerId(TenantId tenantId, UUID customerId) {
        return appointmentJpaRepository
                .findByTenantIdAndCustomerIdAndStatusOrderByStartsAtDesc(
                        tenantId.value(), customerId, AppointmentStatus.COMPLETED)
                .stream()
                .map(AppointmentMapper::toDomain)
                .toList();
    }

    @Override
    public Map<UUID, CustomerActivity> findActivityByCustomerIds(TenantId tenantId, Collection<UUID> customerIds) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return appointmentJpaRepository.findActivityByCustomerIds(tenantId.value(), customerIds).stream()
                .collect(Collectors.toMap(
                        CustomerActivityProjection::getCustomerId,
                        (Function<CustomerActivityProjection, CustomerActivity>) p -> new CustomerActivity(
                                p.getVisitCount(), new Money(p.getTotalCents()), new Money(p.getOwedCents()))));
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
