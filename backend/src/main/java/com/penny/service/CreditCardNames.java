package com.penny.service;

import com.penny.model.CreditCard;

import java.util.List;
import java.util.Optional;

final class CreditCardNames {

    private CreditCardNames() {
    }

    static Optional<CreditCard> sameName(List<CreditCard> cards, String name) {
        String wanted = Names.normalize(name);
        return cards.stream()
            .filter(card -> card.getName().equalsIgnoreCase(name))
            .findFirst()
            .or(() -> cards.stream().filter(card -> Names.normalize(card.getName()).equals(wanted)).findFirst());
    }
}
