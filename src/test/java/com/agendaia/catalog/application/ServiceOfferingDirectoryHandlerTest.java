package com.agendaia.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.catalog.application.port.out.ServiceOfferingRepository;
import com.agendaia.catalog.application.port.out.ServiceRepository;
import com.agendaia.catalog.domain.Service;
import com.agendaia.catalog.domain.ServiceOffering;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.organization.api.ProfessionalRef;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso de catalog, sem Spring e sem banco — port.out e organization.api mockados. */
@ExtendWith(MockitoExtension.class)
class ServiceOfferingDirectoryHandlerTest {

    @Mock private ServiceOfferingRepository serviceOfferingRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private ProfessionalDirectory professionalDirectory;

    private ServiceOfferingDirectoryHandler handler;

    private final TenantId tenant = TenantId.of(UuidV7.generate());

    @BeforeEach
    void montar() {
        handler = new ServiceOfferingDirectoryHandler(serviceOfferingRepository, serviceRepository, professionalDirectory);
        TenantContext.set(tenant);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private ServiceOffering oferta(UUID serviceId, UUID professionalId) {
        return ServiceOffering.register(tenant, serviceId, professionalId, 30, new Money(3000), 0);
    }

    @Test
    @DisplayName("listActiveByService cruza ProfessionalDirectory.listActive() EXATAMENTE uma vez, com 3 ofertas")
    void listActiveByServiceCruzaProfissionalUmaVezSo() {
        var serviceId = UuidV7.generate();
        var prof1 = UuidV7.generate();
        var prof2 = UuidV7.generate();
        var prof3 = UuidV7.generate();

        when(serviceOfferingRepository.findByTenantIdAndServiceIdAndActiveTrueOrderByCreatedAtAsc(
                        tenant.value(), serviceId))
                .thenReturn(List.of(oferta(serviceId, prof1), oferta(serviceId, prof2), oferta(serviceId, prof3)));
        when(professionalDirectory.listActive())
                .thenReturn(List.of(
                        new ProfessionalRef(prof1, "Maria"),
                        new ProfessionalRef(prof2, "Joao"),
                        new ProfessionalRef(prof3, "Ana")));

        var resultado = handler.listActiveByService(serviceId);

        assertThat(resultado).hasSize(3);
        assertThat(resultado).extracting("professionalName").containsExactlyInAnyOrder("Maria", "Joao", "Ana");
        verify(professionalDirectory, times(1)).listActive();
    }

    @Test
    @DisplayName("serviço sem nenhuma oferta ativa devolve lista vazia, sem erro")
    void servicoSemOfertaDevolveListaVazia() {
        var serviceId = UuidV7.generate();
        when(serviceOfferingRepository.findByTenantIdAndServiceIdAndActiveTrueOrderByCreatedAtAsc(
                        tenant.value(), serviceId))
                .thenReturn(List.of());
        when(professionalDirectory.listActive()).thenReturn(List.of());

        assertThat(handler.listActiveByService(serviceId)).isEmpty();
    }

    @Test
    @DisplayName("find resolve o nome do serviço cruzando ServiceRepository (BR-2 da pagina-publica-agendamento)")
    void findResolveNomeDoServico() {
        var serviceId = UuidV7.generate();
        var professionalId = UuidV7.generate();
        var offeringId = UuidV7.generate();
        var offering = ServiceOffering.register(tenant, serviceId, professionalId, 45, new Money(5000), 10);

        when(serviceOfferingRepository.findByTenantIdAndIdAndActiveTrue(tenant.value(), offeringId))
                .thenReturn(Optional.of(offering));
        when(serviceRepository.findById(serviceId))
                .thenReturn(Optional.of(Service.register(tenant, "Corte de Cabelo", null)));

        var resultado = handler.find(offeringId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().serviceName()).isEqualTo("Corte de Cabelo");
        assertThat(resultado.get().price()).isEqualTo(new Money(5000));
        assertThat(resultado.get().durationMinutes()).isEqualTo(45);
    }

    @Test
    @DisplayName("find devolve vazio para oferta inexistente ou de outro tenant, sem chamar ServiceRepository")
    void findVazioParaOfertaInexistente() {
        var offeringId = UuidV7.generate();
        when(serviceOfferingRepository.findByTenantIdAndIdAndActiveTrue(tenant.value(), offeringId))
                .thenReturn(Optional.empty());

        assertThat(handler.find(offeringId)).isEmpty();
    }
}
