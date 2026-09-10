package com.simboraagendar.scheduling.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.simboraagendar.platform.security.SecurityConfig;
import com.simboraagendar.scheduling.application.port.in.CustomerActivityDetail;
import com.simboraagendar.scheduling.application.port.in.CustomerActivityDetailUseCase;
import com.simboraagendar.scheduling.application.port.in.CustomerListEntry;
import com.simboraagendar.scheduling.application.port.in.ListCustomerActivityUseCase;
import com.simboraagendar.scheduling.application.port.in.PagedResult;
import com.simboraagendar.shared.Money;
import com.simboraagendar.shared.UuidV7;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Camada web isolada: os casos de uso são mock, o banco não existe. */
@WebMvcTest(CustomerAdminController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "OWNER")
class CustomerAdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ListCustomerActivityUseCase listCustomerActivity;
    @MockitoBean private CustomerActivityDetailUseCase customerActivityDetail;

    private final UUID customerId = UuidV7.generate();

    @Test
    @DisplayName("GET /admin/clientes lista os clientes da pagina pedida (AC-3)")
    void listaClientesPaginado() throws Exception {
        when(listCustomerActivity.list(eq(1), eq(20)))
                .thenReturn(new PagedResult<>(
                        List.of(new CustomerListEntry(customerId, "Maria", "+5511999990000", 3, false)), 1, 20, 45));

        mockMvc.perform(get("/admin/clientes").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/clientes"))
                .andExpect(content().string(Matchers.containsString("Maria")));
    }

    @Test
    @DisplayName("GET /admin/clientes sinaliza cliente novo (BR-1)")
    void sinalizaClienteNovo() throws Exception {
        when(listCustomerActivity.list(eq(0), eq(20)))
                .thenReturn(new PagedResult<>(
                        List.of(new CustomerListEntry(customerId, "João", "+5511999990000", 0, true)), 0, 20, 1));

        mockMvc.perform(get("/admin/clientes"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Novo")));
    }

    @Test
    @DisplayName("GET /admin/clientes/{id} mostra o historico do cliente")
    void mostraDetalheDoCliente() throws Exception {
        when(customerActivityDetail.detail(customerId))
                .thenReturn(Optional.of(new CustomerActivityDetail(
                        customerId, "Maria", "+5511999990000", 1, new Money(3000), new Money(0), List.of())));

        mockMvc.perform(get("/admin/clientes/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/cliente-detalhe"))
                .andExpect(content().string(Matchers.containsString("Maria")));
    }

    @Test
    @DisplayName("GET /admin/clientes/{id} de outro tenant (ou inexistente) devolve 404 (AC-4)")
    void detalheDeOutroTenantDevolve404() throws Exception {
        when(customerActivityDetail.detail(customerId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/clientes/{id}", customerId)).andExpect(status().isNotFound());
    }
}
