package com.penny.dto;

import com.penny.model.InstallmentPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InstallmentPlanResponse(
    UUID id,
    String description,
    BigDecimal totalAmount,
    Integer installmentsCount,
    LocalDate purchasedOn,
    UUID creditCardId,
    String creditCardName,
    UUID categoryId,
    String categoryName,
    List<ExpenseResponse> installments
) {

    public static InstallmentPlanResponse from(InstallmentPlan plan, List<ExpenseResponse> installments) {
        return new InstallmentPlanResponse(
            plan.getId(),
            plan.getDescription(),
            plan.getTotalAmount(),
            plan.getInstallmentsCount(),
            plan.getPurchasedOn(),
            plan.getCreditCard().getId(),
            plan.getCreditCard().getName(),
            plan.getCategory().getId(),
            plan.getCategory().getName(),
            installments
        );
    }
}
