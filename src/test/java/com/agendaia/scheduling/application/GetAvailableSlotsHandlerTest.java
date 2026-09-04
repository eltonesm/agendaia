package com.agendaia.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.catalog.api.ServiceOfferingRef;
import com.agendaia.organization.api.AvailabilityDirectory;
import com.agendaia.scheduling.application.port.in.GetAvailableSlotsQuery;
import com.agendaia.scheduling.domain.exception.AvailabilityQueryOutOfRangeException;
import com.agendaia.scheduling.domain.exception.ServiceOfferingNotFoundException;
import com.agendaia.shared.TimeRange;
import com.agendaia.shared.UuidV7;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Orquestração isolada, sem Spring e sem banco — organization.api e catalog.api mockados. */
@ExtendWith(MockitoExtension.class)
class GetAvailableSlotsHandlerTest {

    @Mock private ServiceOfferingDirectory serviceOfferingDirectory;
    @Mock private AvailabilityDirectory availabilityDirectory;

    private GetAvailableSlotsHandler handler;

    private final LocalDate hoje = LocalDate.of(2026, 9, 7); // uma segunda-feira

    @BeforeEach
    void montar() {
        handler = new GetAvailableSlotsHandler(serviceOfferingDirectory, availabilityDirectory);
    }

    @Test
    @DisplayName("data igual a hoje é aceita")
    void dataIgualAHojeAceita() {
        var ofertaId = UuidV7.generate();
        var profissionalId = UuidV7.generate();
        when(serviceOfferingDirectory.find(ofertaId))
                .thenReturn(Optional.of(new ServiceOfferingRef(ofertaId, profissionalId, 30, 0)));
        when(availabilityDirectory.operatingHoursFor(DayOfWeek.MONDAY)).thenReturn(List.of());
        when(availabilityDirectory.workScheduleFor(profissionalId, DayOfWeek.MONDAY)).thenReturn(List.of());
        when(availabilityDirectory.blocksFor(profissionalId, hoje)).thenReturn(List.of());

        var resultado = handler.handle(new GetAvailableSlotsQuery(ofertaId, hoje), hoje);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("data igual a hoje + 30 dias e aceita (limite do horizonte)")
    void dataNoLimiteDoHorizonteAceita() {
        var ofertaId = UuidV7.generate();
        var profissionalId = UuidV7.generate();
        var limite = hoje.plusDays(30);
        when(serviceOfferingDirectory.find(ofertaId))
                .thenReturn(Optional.of(new ServiceOfferingRef(ofertaId, profissionalId, 30, 0)));
        when(availabilityDirectory.operatingHoursFor(any())).thenReturn(List.of());
        when(availabilityDirectory.workScheduleFor(any(), any())).thenReturn(List.of());
        when(availabilityDirectory.blocksFor(any(), any())).thenReturn(List.of());

        assertThat(handler.handle(new GetAvailableSlotsQuery(ofertaId, limite), hoje)).isEmpty();
    }

    @Test
    @DisplayName("data um dia depois do horizonte de 30 dias é rejeitada")
    void dataUmDiaAposOHorizonteRejeitada() {
        var ofertaId = UuidV7.generate();
        var query = new GetAvailableSlotsQuery(ofertaId, hoje.plusDays(31));

        assertThatThrownBy(() -> handler.handle(query, hoje)).isInstanceOf(AvailabilityQueryOutOfRangeException.class);
    }

    @Test
    @DisplayName("data no passado é rejeitada, não devolve lista vazia silenciosamente")
    void dataNoPassadoRejeitada() {
        var ofertaId = UuidV7.generate();
        var query = new GetAvailableSlotsQuery(ofertaId, hoje.minusDays(1));

        assertThatThrownBy(() -> handler.handle(query, hoje)).isInstanceOf(AvailabilityQueryOutOfRangeException.class);
    }

    @Test
    @DisplayName("oferta não encontrada (id inexistente ou de outro tenant) lança ServiceOfferingNotFoundException")
    void ofertaNaoEncontrada() {
        var ofertaId = UuidV7.generate();
        when(serviceOfferingDirectory.find(ofertaId)).thenReturn(Optional.empty());
        var query = new GetAvailableSlotsQuery(ofertaId, hoje);

        assertThatThrownBy(() -> handler.handle(query, hoje)).isInstanceOf(ServiceOfferingNotFoundException.class);
    }

    @Test
    @DisplayName("professionalId nunca é aceito do chamador - vem sempre da oferta resolvida")
    void professionalIdVemDaOferta() {
        var ofertaId = UuidV7.generate();
        var profissionalDaOferta = UuidV7.generate();
        when(serviceOfferingDirectory.find(ofertaId))
                .thenReturn(Optional.of(new ServiceOfferingRef(ofertaId, profissionalDaOferta, 30, 0)));
        when(availabilityDirectory.operatingHoursFor(DayOfWeek.MONDAY)).thenReturn(List.of());
        when(availabilityDirectory.blocksFor(profissionalDaOferta, hoje)).thenReturn(List.of());

        handler.handle(new GetAvailableSlotsQuery(ofertaId, hoje), hoje);

        verify(availabilityDirectory).workScheduleFor(profissionalDaOferta, DayOfWeek.MONDAY);
        verify(availabilityDirectory).blocksFor(profissionalDaOferta, hoje);
    }

    @Test
    @DisplayName("combina o TimeRange calculado com a data da consulta para formar o AvailableSlot")
    void combinaTimeRangeComData() {
        var ofertaId = UuidV7.generate();
        var profissionalId = UuidV7.generate();
        when(serviceOfferingDirectory.find(ofertaId))
                .thenReturn(Optional.of(new ServiceOfferingRef(ofertaId, profissionalId, 30, 0)));
        when(availabilityDirectory.operatingHoursFor(DayOfWeek.MONDAY))
                .thenReturn(List.of(new TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0))));
        when(availabilityDirectory.workScheduleFor(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(new TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0))));
        when(availabilityDirectory.blocksFor(profissionalId, hoje)).thenReturn(List.of());

        var resultado = handler.handle(new GetAvailableSlotsQuery(ofertaId, hoje), hoje);

        assertThat(resultado).isNotEmpty();
        var primeiro = resultado.getFirst();
        assertThat(primeiro.professionalId()).isEqualTo(profissionalId);
        assertThat(primeiro.serviceOfferingId()).isEqualTo(ofertaId);
        assertThat(primeiro.startsAt()).isEqualTo(hoje.atTime(8, 0));
        assertThat(primeiro.endsAt()).isEqualTo(hoje.atTime(8, 30));
    }
}
