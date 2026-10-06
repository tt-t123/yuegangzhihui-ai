package com.yuegang.zhihui.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.order.api.OrderStatus;
import com.yuegang.zhihui.order.api.OrderView;
import com.yuegang.zhihui.order.infrastructure.CommerceReconciliationClient;
import com.yuegang.zhihui.wallet.api.WalletReferenceView;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class OrderPaymentConfirmationServiceTest {
    @Test
    void returnsAnOrderThatNoLongerNeedsPaymentWithoutCallingWallet() {
        var orders = mock(OrderService.class);
        var inventory = mock(OrderInventoryFacade.class);
        var reconciliation = mock(CommerceReconciliationClient.class);
        var paid = order(OrderStatus.PAID);
        when(orders.get(42, "1001")).thenReturn(paid);

        var result = new OrderPaymentConfirmationService(orders, inventory, reconciliation)
                .confirmWalletPayment(42, "1001");

        assertThat(result).isSameAs(paid);
        verifyNoInteractions(inventory, reconciliation);
    }

    @Test
    void confirmsAValidWalletPaymentAndReturnsTheUpdatedOrder() {
        var orders = mock(OrderService.class);
        var inventory = mock(OrderInventoryFacade.class);
        var reconciliation = mock(CommerceReconciliationClient.class);
        var pending = order(OrderStatus.PENDING_PAYMENT);
        var paid = order(OrderStatus.PAID);
        when(orders.get(42, "1001")).thenReturn(pending, paid);
        when(reconciliation.wallet("1001")).thenReturn(new WalletReferenceView(
                "1001", "42", true, false, new BigDecimal("25.00"), BigDecimal.ZERO, "CNY"));

        var result = new OrderPaymentConfirmationService(orders, inventory, reconciliation)
                .confirmWalletPayment(42, "1001");

        assertThat(result).isSameAs(paid);
        verify(inventory).paymentSucceeded("wallet-payment-1001", "1001");
    }

    @Test
    void rejectsAConfirmRequestWhenWalletEvidenceDoesNotMatchTheOrder() {
        var orders = mock(OrderService.class);
        var inventory = mock(OrderInventoryFacade.class);
        var reconciliation = mock(CommerceReconciliationClient.class);
        when(orders.get(42, "1001")).thenReturn(order(OrderStatus.PENDING_PAYMENT));
        when(reconciliation.wallet("1001")).thenReturn(new WalletReferenceView(
                "1001", "84", true, false, new BigDecimal("25.00"), BigDecimal.ZERO, "CNY"));

        assertThatThrownBy(() -> new OrderPaymentConfirmationService(orders, inventory, reconciliation)
                .confirmWalletPayment(42, "1001"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT));
        verify(inventory, never()).paymentSucceeded("wallet-payment-1001", "1001");
    }

    private static OrderView order(OrderStatus status) {
        return new OrderView("1001", "YG1001", "42", status, new BigDecimal("25.00"), "CNY", 1,
                OffsetDateTime.parse("2026-08-10T00:00:00Z"));
    }
}
