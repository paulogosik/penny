package com.penny.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(Type type, ExpenseResponse expense, CreditCardResponse creditCard) {

    public enum Type {
        EXPENSE("expense"),
        CREDIT_CARD_CREATED("credit_card_created"),
        CREDIT_CARD_UPDATED("credit_card_updated");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public static MessageResponse expense(ExpenseResponse expense) {
        return new MessageResponse(Type.EXPENSE, expense, null);
    }

    public static MessageResponse creditCard(CreditCardResponse creditCard, boolean created) {
        return new MessageResponse(created ? Type.CREDIT_CARD_CREATED : Type.CREDIT_CARD_UPDATED, null, creditCard);
    }

    public boolean created() {
        return type != Type.CREDIT_CARD_UPDATED;
    }
}
