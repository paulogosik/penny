package com.penny.dto;

import com.penny.model.CreditCard;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditCardResponse(
    UUID id,
    String name,
    Integer closingDay,
    Integer dueDay,
    BigDecimal creditLimit,
    boolean archived
) {

    public static CreditCardResponse from(CreditCard card) {
        return new CreditCardResponse(
            card.getId(),
            card.getName(),
            card.getClosingDay(),
            card.getDueDay(),
            card.getCreditLimit(),
            card.isArchived()
        );
    }
}
