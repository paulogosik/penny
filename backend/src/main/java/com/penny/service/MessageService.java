package com.penny.service;

import com.penny.dto.MessageResponse;
import com.penny.exception.BusinessRuleException;
import com.penny.model.User;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final ExpenseClassifier classifier;
    private final ExpenseService expenseService;
    private final CreditCardService creditCardService;

    public MessageService(ExpenseClassifier classifier, ExpenseService expenseService, CreditCardService creditCardService) {
        this.classifier = classifier;
        this.expenseService = expenseService;
        this.creditCardService = creditCardService;
    }

    public MessageResponse handleText(User user, String text) {
        PhoneNumbers.ensurePresent(user);
        String message = text.strip();
        return switch (classifier.classifyText(message, expenseService.classificationContext(user))) {
            case ClassifiedMessage.ExpenseMessage expense ->
                MessageResponse.expense(expenseService.recordFromText(user, expense.expense(), message));
            case ClassifiedMessage.CreditCardMessage card -> {
                CreditCardService.Registration registration = creditCardService.registerFromMessage(user, card);
                yield MessageResponse.creditCard(registration.card(), registration.created());
            }
            case ClassifiedMessage.Unrecognized unrecognized -> throw new BusinessRuleException(
                "MESSAGE_NOT_UNDERSTOOD",
                "Não entendi. Me mande um gasto, como \"gastei 45 no mercado\", ou cadastre um cartão, "
                    + "como \"meu cartão Nubank fecha dia 3 e vence dia 10\"."
            );
        };
    }
}
