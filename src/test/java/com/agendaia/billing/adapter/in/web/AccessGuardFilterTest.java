package com.agendaia.billing.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.billing.application.BillingAccountService;
import com.agendaia.billing.application.BillingAccountService.AccessSnapshot;
import com.agendaia.billing.domain.AccessStatus;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/** Sem Spring: filtro instanciado direto, request/response/chain mockados. */
@ExtendWith(MockitoExtension.class)
class AccessGuardFilterTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());

    @Mock private BillingAccountService billingAccountService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private AccessGuardFilter filter;

    @BeforeEach
    void montar() {
        ObjectProvider<BillingAccountService> provider = new ObjectProvider<>() {
            @Override
            public BillingAccountService getObject() {
                return billingAccountService;
            }

            @Override
            public BillingAccountService getIfAvailable() {
                return billingAccountService;
            }
        };
        filter = new AccessGuardFilter(provider);
        when(request.getContextPath()).thenReturn("");
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private void caminho(String uri) {
        when(request.getRequestURI()).thenReturn(uri);
    }

    @Test
    @DisplayName("rota fora de /admin segue direto, sem consultar billing")
    void foraDoAdminSegueDireto() throws Exception {
        caminho("/login");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(billingAccountService, never()).snapshotFor(any());
    }

    @Test
    @DisplayName("/admin/conta-suspensa nunca é redirecionada para si mesma (AC-2)")
    void contaSuspensaNuncaRedirecionaParaSiMesma() throws Exception {
        caminho("/admin/conta-suspensa");
        TenantContext.set(TENANT);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
        verify(billingAccountService, never()).snapshotFor(any());
    }

    @Test
    @DisplayName("sem tenant resolvido, segue direto — quem recusa é a autenticação, não este filtro")
    void semTenantSegueDireto() throws Exception {
        caminho("/admin/dashboard");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(billingAccountService, never()).snapshotFor(any());
    }

    @Test
    @DisplayName("BLOCKED redireciona para a tela de conta suspensa e não continua a cadeia")
    void blockedRedireciona() throws Exception {
        caminho("/admin/dashboard");
        TenantContext.set(TENANT);
        when(billingAccountService.snapshotFor(TENANT.value()))
                .thenReturn(new AccessSnapshot(AccessStatus.BLOCKED, LocalDate.now()));

        filter.doFilterInternal(request, response, chain);

        verify(response).sendRedirect("/admin/conta-suspensa");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("GRACE_PERIOD segue a cadeia normalmente, guardando o snapshot para o banner")
    void gracePeriodSegue() throws Exception {
        caminho("/admin/dashboard");
        TenantContext.set(TENANT);
        var snapshot = new AccessSnapshot(AccessStatus.GRACE_PERIOD, LocalDate.now().plusDays(3));
        when(billingAccountService.snapshotFor(TENANT.value())).thenReturn(snapshot);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
        verify(request).setAttribute("billingAccessSnapshot", snapshot);
    }

    @Test
    @DisplayName("TRIAL e PAID seguem sem nenhum efeito colateral")
    void trialEPaidSeguemSemEfeito() throws Exception {
        caminho("/admin/dashboard");
        TenantContext.set(TENANT);
        when(billingAccountService.snapshotFor(TENANT.value()))
                .thenReturn(new AccessSnapshot(AccessStatus.TRIAL, LocalDate.now()));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
    }

    @Test
    @DisplayName("erro inesperado ao calcular status segue sem bloquear — falha aberta")
    void erroInesperadoFalhaAberta() throws Exception {
        caminho("/admin/dashboard");
        TenantContext.set(TENANT);
        when(billingAccountService.snapshotFor(TENANT.value())).thenThrow(new RuntimeException("falha simulada"));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
    }
}
