package com.agendaia.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.TestcontainersConfiguration;
import com.agendaia.catalog.application.port.out.ServiceOfferingRepository;
import com.agendaia.catalog.application.port.out.ServiceRepository;
import com.agendaia.catalog.domain.Service;
import com.agendaia.catalog.domain.ServiceOffering;
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
import com.agendaia.scheduling.application.port.in.GetAvailableSlotsQuery;
import com.agendaia.scheduling.application.port.in.GetAvailableSlotsUseCase;
import com.agendaia.scheduling.domain.exception.ServiceOfferingNotFoundException;
import com.agendaia.shared.Money;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * E2E-1 a E2E-4 da spec funcional de consultar-horarios-disponiveis, ponta a
 * ponta contra Postgres real, chamando {@link GetAvailableSlotsUseCase}
 * diretamente — sem camada web, conforme decisão da spec funcional
 * (Assumptions: "esta feature não é exposta por nenhum controller HTTP").
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConsultarDisponibilidadeIT {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    // Sempre a próxima segunda-feira estritamente depois de hoje — nunca uma
    // data fixa: GetAvailableSlotsHandler valida contra LocalDate.now() de
    // verdade (BR-5), então uma data hardcoded viraria passado e quebraria o
    // teste assim que o tempo real passasse dela.
    private static final LocalDate SEGUNDA = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Autowired private GetAvailableSlotsUseCase getAvailableSlotsUseCase;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private ServiceOfferingRepository serviceOfferingRepository;
    @Autowired private BusinessOperatingHoursRepository businessOperatingHoursRepository;
    @Autowired private WorkScheduleRepository workScheduleRepository;
    @Autowired private TimeOffRepository timeOffRepository;

    private Business barbearia;
    private Professional maria;
    private ServiceOffering ofertaDaMaria;

    @BeforeEach
    void semear() {
        timeOffRepository.deleteAllInBatch();
        workScheduleRepository.deleteAllInBatch();
        businessOperatingHoursRepository.deleteAllInBatch();
        serviceOfferingRepository.deleteAllInBatch();
        serviceRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
        businessRepository.deleteAllInBatch();

        barbearia = businessRepository.saveAndFlush(
                Business.register("Barbearia do João", "barbearia-do-joao-disponibilidade"));
        maria = professionalRepository.saveAndFlush(Professional.register(barbearia.tenantId(), "Maria Oliveira"));
        var corte = serviceRepository.saveAndFlush(Service.register(barbearia.tenantId(), "Corte de Cabelo", null));
        ofertaDaMaria = serviceOfferingRepository.saveAndFlush(
                ServiceOffering.register(barbearia.tenantId(), corte.id(), maria.id(), 30, Money.reais(new java.math.BigDecimal("30.00")), 0));
        businessOperatingHoursRepository.saveAndFlush(
                BusinessOperatingHours.register(barbearia.tenantId(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)));
        workScheduleRepository.saveAndFlush(WorkSchedule.register(
                barbearia.tenantId(), maria.id(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        workScheduleRepository.saveAndFlush(WorkSchedule.register(
                barbearia.tenantId(), maria.id(), DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(18, 0)));

        TenantContext.set(barbearia.tenantId());
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private static Instant instantNaSegunda(int hora, int minuto) {
        return SEGUNDA.atTime(hora, minuto).atZone(ZONE).toInstant();
    }

    @Test
    @DisplayName("E2E-1: caminho feliz, grade de 10 min respeitando o intervalo de almoço")
    void caminhoFeliz() {
        var resultado = getAvailableSlotsUseCase.handle(new GetAvailableSlotsQuery(ofertaDaMaria.id(), SEGUNDA));

        assertThat(resultado).hasSize(50); // 22 candidatos de manhã + 28 à tarde (ver SlotCalculatorTest)
        assertThat(resultado).noneMatch(slot -> slot.startsAt().toLocalTime().isAfter(LocalTime.of(11, 30))
                && slot.startsAt().toLocalTime().isBefore(LocalTime.of(13, 0)));
        assertThat(resultado.getFirst().startsAt()).isEqualTo(SEGUNDA.atTime(8, 0));
        assertThat(resultado.getLast().startsAt()).isEqualTo(SEGUNDA.atTime(17, 30));
        assertThat(resultado).allMatch(slot -> slot.professionalId().equals(maria.id()));
        assertThat(resultado).allMatch(slot -> slot.serviceOfferingId().equals(ofertaDaMaria.id()));
    }

    @Test
    @DisplayName("E2E-2: bloqueio do profissional remove só a janela bloqueada")
    void bloqueioDoProfissionalRemoveSoAJanela() {
        timeOffRepository.saveAndFlush(TimeOff.register(
                barbearia.tenantId(), maria.id(), instantNaSegunda(10, 0), instantNaSegunda(11, 0), "Consulta"));

        var resultado = getAvailableSlotsUseCase.handle(new GetAvailableSlotsQuery(ofertaDaMaria.id(), SEGUNDA));

        assertThat(resultado).noneMatch(slot -> !slot.startsAt().toLocalTime().isBefore(LocalTime.of(10, 0))
                && slot.startsAt().toLocalTime().isBefore(LocalTime.of(11, 0)));
        assertThat(resultado).anyMatch(slot -> slot.startsAt().toLocalTime().equals(LocalTime.of(9, 30)));
        assertThat(resultado).anyMatch(slot -> slot.startsAt().toLocalTime().equals(LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("E2E-3: feriado do estabelecimento zera todos os profissionais")
    void feriadoDoEstabelecimentoZeraTodosOsProfissionais() {
        var pedro = professionalRepository.saveAndFlush(Professional.register(barbearia.tenantId(), "Pedro Souza"));
        var corte = serviceRepository.findByTenantIdAndActiveTrueOrderByNameAsc(barbearia.id()).getFirst();
        var ofertaDoPedro = serviceOfferingRepository.saveAndFlush(ServiceOffering.register(
                barbearia.tenantId(), corte.id(), pedro.id(), 30, Money.reais(new java.math.BigDecimal("30.00")), 0));
        workScheduleRepository.saveAndFlush(WorkSchedule.register(
                barbearia.tenantId(), pedro.id(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)));

        timeOffRepository.saveAndFlush(TimeOff.register(
                barbearia.tenantId(),
                null,
                SEGUNDA.atStartOfDay(ZONE).toInstant(),
                SEGUNDA.plusDays(1).atStartOfDay(ZONE).toInstant(),
                "Feriado"));

        assertThat(getAvailableSlotsUseCase.handle(new GetAvailableSlotsQuery(ofertaDaMaria.id(), SEGUNDA)))
                .isEmpty();
        assertThat(getAvailableSlotsUseCase.handle(new GetAvailableSlotsQuery(ofertaDoPedro.id(), SEGUNDA)))
                .isEmpty();
    }

    @Test
    @DisplayName("E2E-4: oferta de outro tenant é recusada, mesmo com id válido")
    void ofertaDeOutroTenantERecusada() {
        var salao = businessRepository.saveAndFlush(Business.register("Salão da Maria", "salao-da-maria-disponibilidade"));
        var anaDoSalao = professionalRepository.saveAndFlush(Professional.register(salao.tenantId(), "Ana"));
        var serviceDoSalao = serviceRepository.saveAndFlush(Service.register(salao.tenantId(), "Manicure", null));
        var ofertaDoSalao = serviceOfferingRepository.saveAndFlush(ServiceOffering.register(
                salao.tenantId(), serviceDoSalao.id(), anaDoSalao.id(), 30, Money.reais(new java.math.BigDecimal("30.00")), 0));

        // TenantContext ainda é o da barbearia (@BeforeEach) — consulta usa o id
        // da oferta do salão, de outro tenant.
        var query = new GetAvailableSlotsQuery(ofertaDoSalao.id(), SEGUNDA);

        assertThatThrownBy(() -> getAvailableSlotsUseCase.handle(query)).isInstanceOf(ServiceOfferingNotFoundException.class);
    }
}
