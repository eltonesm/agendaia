package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.organization.api.AvailabilityDirectory;
import com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository;
import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.TimeOffRepository;
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessOperatingHours;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.TimeOff;
import com.agendaia.organization.domain.WorkSchedule;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TimeRange;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Postgres real via Testcontainers — foco na conversão {@code Instant} →
 * {@link TimeRange} recortada ao dia consultado ({@link #blocksFor}), único
 * ponto do projeto que faz essa conversão de fuso (DD-4).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AvailabilityDirectoryHandlerTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Autowired private AvailabilityDirectory availabilityDirectory;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private BusinessOperatingHoursRepository businessOperatingHoursRepository;
    @Autowired private WorkScheduleRepository workScheduleRepository;
    @Autowired private TimeOffRepository timeOffRepository;

    private Business barbearia;
    private Professional maria;

    @BeforeEach
    void semear() {
        timeOffRepository.deleteAllInBatch();
        workScheduleRepository.deleteAllInBatch();
        businessOperatingHoursRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();

        barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia do João", "barbearia-do-joao-availability"));
        maria = professionalRepository.saveAndFlush(Professional.register(barbearia.tenantId(), "Maria Oliveira"));

        TenantContext.set(barbearia.tenantId());
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private static java.time.Instant instant(String isoLocalDateTime) {
        return LocalDateTime.parse(isoLocalDateTime).atZone(ZONE).toInstant();
    }

    @Test
    @DisplayName("operatingHoursFor devolve as faixas do dia da semana convertidas para TimeRange")
    void operatingHoursForConverteParaTimeRange() {
        businessOperatingHoursRepository.saveAndFlush(
                BusinessOperatingHours.register(barbearia.tenantId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)));

        var faixas = availabilityDirectory.operatingHoursFor(DayOfWeek.MONDAY);

        assertThat(faixas).containsExactly(new TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)));
    }

    @Test
    @DisplayName("workScheduleFor devolve só as faixas do profissional consultado")
    void workScheduleForFiltraPorProfissional() {
        var outroProfissional = professionalRepository.saveAndFlush(Professional.register(barbearia.tenantId(), "Ana"));
        workScheduleRepository.saveAndFlush(
                WorkSchedule.register(barbearia.tenantId(), maria.id(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        workScheduleRepository.saveAndFlush(WorkSchedule.register(
                barbearia.tenantId(), outroProfissional.id(), DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));

        var faixas = availabilityDirectory.workScheduleFor(maria.id(), DayOfWeek.MONDAY);

        assertThat(faixas).containsExactly(new TimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("AC-1: TimeOff que cruza a meia-noite é recortado para as bordas do dia consultado")
    void timeOffQueCruzaAMeiaNoiteERecortado() {
        // Bloqueio das 22:00 de um dia até as 02:00 do dia seguinte.
        timeOffRepository.saveAndFlush(TimeOff.register(
                barbearia.tenantId(), maria.id(), instant("2026-09-07T22:00:00"), instant("2026-09-08T02:00:00"), "Viagem"));

        var noDiaQueComeca = availabilityDirectory.blocksFor(maria.id(), LocalDate.of(2026, 9, 7));
        var noDiaQueTermina = availabilityDirectory.blocksFor(maria.id(), LocalDate.of(2026, 9, 8));

        assertThat(noDiaQueComeca).containsExactly(new TimeRange(LocalTime.of(22, 0), LocalTime.MAX));
        assertThat(noDiaQueTermina).containsExactly(new TimeRange(LocalTime.MIN, LocalTime.of(2, 0)));
    }

    @Test
    @DisplayName("AC-2: bloqueio geral aparece junto com o bloqueio do profissional específico")
    void bloqueioGeralApareceJuntoComOEspecifico() {
        timeOffRepository.saveAndFlush(TimeOff.register(
                barbearia.tenantId(), maria.id(), instant("2026-09-07T09:00:00"), instant("2026-09-07T10:00:00"), "Consulta"));
        timeOffRepository.saveAndFlush(TimeOff.register(
                barbearia.tenantId(), null, instant("2026-09-07T00:00:00"), instant("2026-09-08T00:00:00"), "Feriado"));

        var faixas = availabilityDirectory.blocksFor(maria.id(), LocalDate.of(2026, 9, 7));

        assertThat(faixas)
                .containsExactlyInAnyOrder(
                        new TimeRange(LocalTime.of(9, 0), LocalTime.of(10, 0)), new TimeRange(LocalTime.MIN, LocalTime.MAX));
    }

    @Test
    @DisplayName("blocksFor não traz bloqueio de outro profissional")
    void blocksForNaoTrazBloqueioDeOutroProfissional() {
        var outroProfissional = professionalRepository.saveAndFlush(Professional.register(barbearia.tenantId(), "Ana"));
        timeOffRepository.saveAndFlush(TimeOff.register(
                barbearia.tenantId(),
                outroProfissional.id(),
                instant("2026-09-07T09:00:00"),
                instant("2026-09-07T10:00:00"),
                "Consulta da Ana"));

        assertThat(availabilityDirectory.blocksFor(maria.id(), LocalDate.of(2026, 9, 7))).isEmpty();
    }
}
