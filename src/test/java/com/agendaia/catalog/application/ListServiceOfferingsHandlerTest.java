package com.agendaia.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.catalog.domain.Service;
import com.agendaia.catalog.domain.ServiceOffering;
import com.agendaia.catalog.domain.ServiceOfferingRepository;
import com.agendaia.catalog.domain.ServiceRepository;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.organization.api.ProfessionalRef;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso da listagem de ofertas, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class ListServiceOfferingsHandlerTest {

    @Mock private ServiceOfferingRepository serviceOfferingRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private ProfessionalDirectory professionalDirectory;

    private ListServiceOfferingsHandler handler;

    @BeforeEach
    void montar() {
        handler = new ListServiceOfferingsHandler(serviceOfferingRepository, serviceRepository, professionalDirectory);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("resolve nome do servico e do profissional na projecao, nao ids crus (AC-5)")
    void resolveNomesNaProjecao() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        var servico = Service.register(tenant, "Corte de Cabelo", null);
        var profissionalId = UuidV7.generate();
        var oferta = ServiceOffering.register(
                tenant, servico.id(), profissionalId, 30, Money.reais(new BigDecimal("30.00")), 10);

        when(serviceOfferingRepository.findByTenantIdAndActiveTrueOrderByCreatedAtAsc(tenant.value()))
                .thenReturn(List.of(oferta));
        when(serviceRepository.findAllById(List.of(servico.id()))).thenReturn(List.of(servico));
        when(professionalDirectory.listActive())
                .thenReturn(List.of(new ProfessionalRef(profissionalId, "João da Silva")));

        var lista = handler.list();

        assertThat(lista).hasSize(1);
        var linha = lista.getFirst();
        assertThat(linha.id()).isEqualTo(oferta.id());
        assertThat(linha.serviceName()).isEqualTo("Corte de Cabelo");
        assertThat(linha.professionalName()).isEqualTo("João da Silva");
        assertThat(linha.durationMinutes()).isEqualTo(30);
        assertThat(linha.priceFormatted()).isEqualTo("R$ 30,00");
    }

    @Test
    @DisplayName("chama listActive() no maximo uma vez, mesmo com varias ofertas")
    void listActiveChamadoUmaVezSo() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        var servico = Service.register(tenant, "Corte de Cabelo", null);
        var profissional1 = UuidV7.generate();
        var profissional2 = UuidV7.generate();
        var preco = Money.reais(new BigDecimal("30.00"));
        var oferta1 = ServiceOffering.register(tenant, servico.id(), profissional1, 30, preco, 0);
        var oferta2 = ServiceOffering.register(tenant, servico.id(), profissional2, 45, preco, 0);

        when(serviceOfferingRepository.findByTenantIdAndActiveTrueOrderByCreatedAtAsc(tenant.value()))
                .thenReturn(List.of(oferta1, oferta2));
        when(serviceRepository.findAllById(List.of(servico.id()))).thenReturn(List.of(servico));
        when(professionalDirectory.listActive())
                .thenReturn(List.of(
                        new ProfessionalRef(profissional1, "João"), new ProfessionalRef(profissional2, "Maria")));

        var lista = handler.list();

        assertThat(lista).hasSize(2);
        verify(professionalDirectory, times(1)).listActive();
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de devolver a lista de alguém")
    void semTenantRecusa() {
        assertThatThrownBy(handler::list)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");
    }

    @Test
    @DisplayName("estabelecimento sem oferta devolve lista vazia, nao erro")
    void semOfertaDevolveListaVazia() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(serviceOfferingRepository.findByTenantIdAndActiveTrueOrderByCreatedAtAsc(tenant.value()))
                .thenReturn(List.of());
        when(serviceRepository.findAllById(List.of())).thenReturn(List.of());
        when(professionalDirectory.listActive()).thenReturn(List.of());

        assertThat(handler.list()).isEmpty();
    }
}
