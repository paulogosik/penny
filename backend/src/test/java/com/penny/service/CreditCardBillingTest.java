package com.penny.service;

import com.penny.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditCardBillingTest {

    @Test
    void purchaseUpToClosingDayGoesToCurrentStatementWhenDueAfterClosing() {
        assertThat(CreditCardBilling.billingMonth(3, 10, LocalDate.of(2026, 10, 2))).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(CreditCardBilling.billingMonth(3, 10, LocalDate.of(2026, 10, 3))).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(CreditCardBilling.billingMonth(3, 10, LocalDate.of(2026, 10, 4))).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void dueDayBeforeClosingDayPushesPaymentToNextMonth() {
        assertThat(CreditCardBilling.billingMonth(25, 5, LocalDate.of(2026, 10, 20))).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(CreditCardBilling.billingMonth(25, 5, LocalDate.of(2026, 10, 26))).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void closingDayBeyondMonthLengthUsesLastDayOfMonth() {
        assertThat(CreditCardBilling.billingMonth(31, 10, LocalDate.of(2026, 2, 28))).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(CreditCardBilling.billingMonth(31, 10, LocalDate.of(2026, 3, 1))).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    void dueDateIsClampedToMonthLength() {
        assertThat(CreditCardBilling.dueDate(31, LocalDate.of(2026, 2, 1))).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(CreditCardBilling.dueDate(10, LocalDate.of(2026, 11, 1))).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void lastInstallmentAbsorbsRoundingDifference() {
        assertThat(CreditCardBilling.splitInstallments(new BigDecimal("100.00"), 3))
            .containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
        List<BigDecimal> even = CreditCardBilling.splitInstallments(new BigDecimal("10.00"), 4);
        assertThat(even).containsOnly(new BigDecimal("2.50"));
        assertThat(even.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("10.00");
    }

    @Test
    void amountTooSmallForInstallmentsIsRejected() {
        assertThatThrownBy(() -> CreditCardBilling.splitInstallments(new BigDecimal("0.02"), 3))
            .isInstanceOf(BusinessRuleException.class);
    }
}
