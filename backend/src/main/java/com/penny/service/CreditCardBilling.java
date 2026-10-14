package com.penny.service;

import com.penny.exception.BusinessRuleException;
import com.penny.model.CreditCard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class CreditCardBilling {

    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    private CreditCardBilling() {
    }

    public static LocalDate billingMonth(CreditCard card, LocalDate purchaseDate) {
        return billingMonth(card.getClosingDay(), card.getDueDay(), purchaseDate);
    }

    public static LocalDate billingMonth(int closingDay, int dueDay, LocalDate purchaseDate) {
        YearMonth purchaseMonth = YearMonth.from(purchaseDate);
        int effectiveClosingDay = Math.min(closingDay, purchaseMonth.lengthOfMonth());
        YearMonth closingMonth = purchaseDate.getDayOfMonth() <= effectiveClosingDay
            ? purchaseMonth
            : purchaseMonth.plusMonths(1);
        YearMonth dueMonth = dueDay > closingDay ? closingMonth : closingMonth.plusMonths(1);
        return dueMonth.atDay(1);
    }

    public static LocalDate dueDate(int dueDay, LocalDate billingMonth) {
        YearMonth month = YearMonth.from(billingMonth);
        return month.atDay(Math.min(dueDay, month.lengthOfMonth()));
    }

    public static List<BigDecimal> splitInstallments(BigDecimal total, int count) {
        if (total.compareTo(ONE_CENT.multiply(BigDecimal.valueOf(count))) < 0) {
            throw new BusinessRuleException(
                "INSTALLMENT_TOO_SMALL",
                "O valor é pequeno demais para ser dividido em " + count + " parcelas."
            );
        }
        BigDecimal base = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal last = total.subtract(base.multiply(BigDecimal.valueOf(count - 1)));
        List<BigDecimal> amounts = new ArrayList<>(count);
        for (int i = 0; i < count - 1; i++) {
            amounts.add(base);
        }
        amounts.add(last);
        return amounts;
    }
}
