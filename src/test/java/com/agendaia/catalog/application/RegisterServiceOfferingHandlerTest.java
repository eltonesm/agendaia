package com.agendaia.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.catalog.application.command.RegisterServiceOfferingCommand;
import com.agendaia.catalog.application.port.out.ServiceOfferingRepository;
import com.agendaia.catalog.application.port.out.ServiceRepository;
import com.agendaia.catalog.domain.ServiceOffering;
import com.agendaia.catalog.domain.exception.ProfessionalNotFoundException;
import com.agendaia.catalog.domain.exception.ServiceNotFoundException;
import com.agendaia.catalog.domain.exception.ServiceOfferingAlreadyExistsException;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.organization.api.ProfessionalRef;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Caso de uso do cadastro de oferta, sem Spring e sem banco.
 *
 * <p>{@link ProfessionalDirectory} mockado — confirma a validação de
 * fronteira entre contextos (BR-8) sem precisar de banco (AC-4 do TASK-007).
 */
@ExtendWith(MockitoExtension.class)
class RegisterServiceOfferingHandlerTest {

    @Mock private ServiceOfferingRepository serviceOfferingRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private ProfessionalDirectory professionalDirectory;

    private RegisterServiceOfferingHandler handler;

    private final UUID serviceId = UuidV7.generate();
    private final UUID professionalId = UuidV7.generate();
    private final Money preco = Money.reais(new BigDecimal("30.00"));

    @BeforeEach
    void montar() {
        handler = new RegisterServiceOfferingHandler(serviceOfferingRepository, serviceRepository, professionalDirectory);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private RegisterServiceOfferingCommand comando() {
        return new RegisterServiceOfferingCommand(serviceId, professionalId, 30, preco, 10);
    }

    @Test
    @DisplayName("cadastra quando servico e profissional pertencem ao tenant, sem duplicata")
    void cadastraComSucesso() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(serviceRepository.existsByIdAndTenantId(serviceId, tenant.value())).thenReturn(true);
        when(professionalDirectory.listActive()).thenReturn(List.of(new ProfessionalRef(professionalId, "João")));
        when(serviceOfferingRepository.existsByTenantIdAndServiceIdAndProfessionalId(
                        tenant.value(), serviceId, professionalId))
                .thenReturn(false);

        var resultado = handler.register(comando());

        assertThat(resultado.id()).isNotNull();
        var captor = ArgumentCaptor.forClass(ServiceOffering.class);
        verify(serviceOfferingRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenant);
    }

    @Test
    @DisplayName("recusa profissional fora da lista de listActive() (BR-8) — mesmo erro de 'nao encontrado'")
    void recusaProfissionalForaDaLista() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(serviceRepository.existsByIdAndTenantId(serviceId, tenant.value())).thenReturn(true);
        when(professionalDirectory.listActive()).thenReturn(List.of());

        assertThatThrownBy(() -> handler.register(comando()))
                .isInstanceOf(ProfessionalNotFoundException.class);

        verify(serviceOfferingRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("recusa servico que nao pertence ao tenant da sessao")
    void recusaServicoDeOutroTenant() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(serviceRepository.existsByIdAndTenantId(serviceId, tenant.value())).thenReturn(false);

        assertThatThrownBy(() -> handler.register(comando())).isInstanceOf(ServiceNotFoundException.class);

        verify(serviceOfferingRepository, never()).saveAndFlush(any());
        verify(professionalDirectory, never()).listActive();
    }

    @Test
    @DisplayName("recusa segunda oferta do mesmo (servico, profissional) — BR-7")
    void recusaOfertaDuplicada() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(serviceRepository.existsByIdAndTenantId(serviceId, tenant.value())).thenReturn(true);
        when(professionalDirectory.listActive()).thenReturn(List.of(new ProfessionalRef(professionalId, "João")));
        when(serviceOfferingRepository.existsByTenantIdAndServiceIdAndProfessionalId(
                        tenant.value(), serviceId, professionalId))
                .thenReturn(true);

        assertThatThrownBy(() -> handler.register(comando()))
                .isInstanceOf(ServiceOfferingAlreadyExistsException.class);

        verify(serviceOfferingRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de gravar para ninguém")
    void semTenantRecusa() {
        assertThatThrownBy(() -> handler.register(comando()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");

        verify(serviceOfferingRepository, never()).saveAndFlush(any());
    }
}
