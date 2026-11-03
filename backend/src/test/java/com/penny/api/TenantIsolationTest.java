package com.penny.api;

import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantIsolationTest extends IntegrationTest {

    @Test
    void anotherUsersExpensesAndIncomesCannotBeReadChangedOrRemoved() throws Exception {
        long owner = registerUser();
        long intruder = registerUser();
        String expenseId = created(postAs(owner, "/api/expenses", expenseJson(categoryId(owner, "Mercado"), "50.00", null)));
        String incomeId = created(postAs(owner, "/api/incomes", incomeJson(incomeCategoryId(owner, "Salário"), "3000.00")));

        String intruderExpense = expenseJson(categoryId(intruder, "Mercado"), "1.00", null);
        getAs(intruder, "/api/expenses/" + expenseId)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("EXPENSE_NOT_FOUND"));
        putAs(intruder, "/api/expenses/" + expenseId, intruderExpense).andExpect(status().isNotFound());
        deleteAs(intruder, "/api/expenses/" + expenseId).andExpect(status().isNotFound());

        String intruderIncome = incomeJson(incomeCategoryId(intruder, "Salário"), "1.00");
        getAs(intruder, "/api/incomes/" + incomeId)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("INCOME_NOT_FOUND"));
        putAs(intruder, "/api/incomes/" + incomeId, intruderIncome).andExpect(status().isNotFound());
        deleteAs(intruder, "/api/incomes/" + incomeId).andExpect(status().isNotFound());

        getAs(owner, "/api/expenses/" + expenseId).andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(50.00));
        getAs(owner, "/api/incomes/" + incomeId).andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(3000.00));
    }

    @Test
    void anotherUsersCategoriesAndCardsCannotBeChangedOrRemoved() throws Exception {
        long owner = registerUser();
        long intruder = registerUser();
        String categoryId = categoryId(owner, "Mercado");
        String incomeCategoryId = incomeCategoryId(owner, "Salário");
        String cardId = createCard(owner, "Nubank", 3, 10);

        putAs(intruder, "/api/expense-categories/" + categoryId, "{\"name\":\"Invadida\"}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        deleteAs(intruder, "/api/expense-categories/" + categoryId).andExpect(status().isNotFound());
        putAs(intruder, "/api/income-categories/" + incomeCategoryId, "{\"name\":\"Invadida\"}").andExpect(status().isNotFound());
        deleteAs(intruder, "/api/income-categories/" + incomeCategoryId).andExpect(status().isNotFound());
        putAs(intruder, "/api/credit-cards/" + cardId, "{\"name\":\"Invadido\",\"closingDay\":1,\"dueDay\":5}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_FOUND"));
        deleteAs(intruder, "/api/credit-cards/" + cardId).andExpect(status().isNotFound());
        getAs(intruder, "/api/credit-cards/" + cardId + "/statements/2026-11").andExpect(status().isNotFound());

        getAs(owner, "/api/expense-categories")
            .andExpect(jsonPath("$[?(@.name == 'Mercado')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Invadida')]").doesNotExist());
        getAs(owner, "/api/income-categories").andExpect(jsonPath("$[?(@.name == 'Salário')]").exists());
        getAs(owner, "/api/credit-cards")
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Nubank"))
            .andExpect(jsonPath("$[0].closingDay").value(3));
    }

    @Test
    void anotherUsersRecurringRulesGoalsAndInstallmentPlansAreNotFound() throws Exception {
        long owner = registerUser();
        long intruder = registerUser();
        String cardId = createCard(owner, "Inter", 5, 12);
        String recurringExpenseId = created(postAs(owner, "/api/recurring-expenses", recurringExpenseJson(categoryId(owner, "Moradia"), null)));
        String recurringIncomeId = created(postAs(owner, "/api/recurring-incomes", recurringIncomeJson(incomeCategoryId(owner, "Salário"))));
        String goalId = created(postAs(owner, "/api/goals", "{\"name\":\"Viagem\",\"targetAmount\":5000}"));
        String planId = created(postAs(owner, "/api/installment-plans", planJson(cardId, categoryId(owner, "Compras"))));

        putAs(intruder, "/api/recurring-expenses/" + recurringExpenseId, recurringExpenseJson(categoryId(intruder, "Moradia"), null))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RECURRING_EXPENSE_NOT_FOUND"));
        deleteAs(intruder, "/api/recurring-expenses/" + recurringExpenseId).andExpect(status().isNotFound());
        putAs(intruder, "/api/recurring-incomes/" + recurringIncomeId, recurringIncomeJson(incomeCategoryId(intruder, "Salário")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RECURRING_INCOME_NOT_FOUND"));
        deleteAs(intruder, "/api/recurring-incomes/" + recurringIncomeId).andExpect(status().isNotFound());
        putAs(intruder, "/api/goals/" + goalId, "{\"name\":\"Invadida\",\"targetAmount\":1}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("GOAL_NOT_FOUND"));
        deleteAs(intruder, "/api/goals/" + goalId).andExpect(status().isNotFound());
        getAs(intruder, "/api/installment-plans/" + planId)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("INSTALLMENT_PLAN_NOT_FOUND"));
        deleteAs(intruder, "/api/installment-plans/" + planId).andExpect(status().isNotFound());

        getAs(owner, "/api/recurring-expenses").andExpect(jsonPath("$.length()").value(1));
        getAs(owner, "/api/recurring-incomes").andExpect(jsonPath("$.length()").value(1));
        getAs(owner, "/api/goals").andExpect(jsonPath("$[0].name").value("Viagem"));
        getAs(owner, "/api/installment-plans/" + planId).andExpect(jsonPath("$.installments.length()").value(3));
    }

    @Test
    void anotherUsersCategoriesAndCardsCannotBeUsedInNewEntries() throws Exception {
        long owner = registerUser();
        long intruder = registerUser();
        String ownerCategory = categoryId(owner, "Mercado");
        String ownerIncomeCategory = incomeCategoryId(owner, "Salário");
        String ownerCard = createCard(owner, "Nubank", 3, 10);
        String intruderCategory = categoryId(intruder, "Mercado");

        postAs(intruder, "/api/expenses", expenseJson(intruderCategory, "10.00", ownerCard))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_FOUND"));
        postAs(intruder, "/api/incomes", incomeJson(ownerIncomeCategory, "10.00"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        postAs(intruder, "/api/recurring-expenses", recurringExpenseJson(ownerCategory, null))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        postAs(intruder, "/api/recurring-expenses", recurringExpenseJson(intruderCategory, ownerCard))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_FOUND"));
        postAs(intruder, "/api/recurring-incomes", recurringIncomeJson(ownerIncomeCategory))
            .andExpect(status().isNotFound());
        postAs(intruder, "/api/installment-plans", planJson(ownerCard, intruderCategory))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_FOUND"));

        String intruderExpense = created(postAs(intruder, "/api/expenses", expenseJson(intruderCategory, "10.00", null)));
        putAs(intruder, "/api/expenses/" + intruderExpense, expenseJson(ownerCategory, "10.00", null))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        getAs(intruder, "/api/expenses").andExpect(jsonPath("$.totalElements").value(1));
        getAs(intruder, "/api/incomes").andExpect(jsonPath("$.length()").value(0));
        getAs(intruder, "/api/recurring-expenses").andExpect(jsonPath("$.length()").value(0));
        getAs(intruder, "/api/installment-plans").andExpect(jsonPath("$.length()").value(0));
        getAs(owner, "/api/credit-cards/" + ownerCard + "/statements/2026-11").andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void listsFiltersAndDashboardOnlyShowTheCallersData() throws Exception {
        long owner = registerUser();
        long intruder = registerUser();
        String ownerCategory = categoryId(owner, "Mercado");
        String ownerCard = createCard(owner, "Nubank", 3, 10);
        postAs(owner, "/api/expenses", expenseJson(ownerCategory, "80.00", ownerCard)).andExpect(status().isCreated());
        postAs(owner, "/api/incomes", incomeJson(incomeCategoryId(owner, "Salário"), "5000.00")).andExpect(status().isCreated());
        postAs(owner, "/api/goals", "{\"name\":\"Reserva\",\"targetAmount\":1000}").andExpect(status().isCreated());

        getAs(intruder, "/api/expenses?categoryId=" + ownerCategory).andExpect(jsonPath("$.totalElements").value(0));
        getAs(intruder, "/api/expenses?creditCardId=" + ownerCard).andExpect(jsonPath("$.totalElements").value(0));
        getAs(intruder, "/api/expenses?billingMonth=2026-11").andExpect(jsonPath("$.totalElements").value(0));
        getAs(intruder, "/api/incomes").andExpect(jsonPath("$.length()").value(0));
        getAs(intruder, "/api/goals").andExpect(jsonPath("$.length()").value(0));
        getAs(intruder, "/api/credit-cards?includeArchived=true").andExpect(jsonPath("$.length()").value(0));
        getAs(intruder, "/api/dashboard?month=2026-10")
            .andExpect(jsonPath("$.totalExpenses").value(0))
            .andExpect(jsonPath("$.totalIncome").value(0))
            .andExpect(jsonPath("$.creditCardBills.length()").value(0));

        getAs(owner, "/api/dashboard?month=2026-10")
            .andExpect(jsonPath("$.totalExpenses").value(80.00))
            .andExpect(jsonPath("$.totalIncome").value(5000.00));
    }

    @Test
    void aiCannotFileAnExpenseUnderAnotherUsersCategoryOrCard() throws Exception {
        long owner = registerUser();
        long intruder = registerUser();
        postAs(owner, "/api/expense-categories", "{\"name\":\"Viagem\"}").andExpect(status().isCreated());
        createCard(owner, "Nubank", 3, 10);
        classifier.willReturn("Viagem", "900.00", LocalDate.of(2026, 10, 13), "Passagem", 1, "Nubank");

        postAs(intruder, "/api/messages/text", "{\"text\":\"passagem 900 no nubank\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("CREDIT_CARD_NOT_REGISTERED"));

        assertThat(classifier.lastContext().categories()).doesNotContain("Viagem");
        assertThat(classifier.lastContext().creditCards()).isEmpty();

        classifier.willReturn("Viagem", "900.00", LocalDate.of(2026, 10, 13), "Passagem");
        postAs(intruder, "/api/messages/text", "{\"text\":\"passagem 900\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expense.categoryName").value("Outros"));

        getAs(intruder, "/api/expenses").andExpect(jsonPath("$.totalElements").value(1));
        getAs(owner, "/api/expenses").andExpect(jsonPath("$.totalElements").value(0));
    }

    private String created(ResultActions result) throws Exception {
        return body(result.andExpect(status().isCreated())).path("id").asText();
    }

    private static String expenseJson(String categoryId, String amount, String creditCardId) {
        return "{\"categoryId\":\"%s\",\"amount\":%s,\"description\":\"Compra\",\"occurredOn\":\"2026-10-13\",\"creditCardId\":%s}"
            .formatted(categoryId, amount, creditCardId == null ? "null" : "\"" + creditCardId + "\"");
    }

    private static String incomeJson(String categoryId, String amount) {
        return "{\"categoryId\":\"%s\",\"amount\":%s,\"description\":\"Pagamento\",\"receivedOn\":\"2026-10-05\"}"
            .formatted(categoryId, amount);
    }

    private static String recurringExpenseJson(String categoryId, String creditCardId) {
        return ("{\"categoryId\":\"%s\",\"creditCardId\":%s,\"description\":\"Aluguel\",\"amount\":1500,"
            + "\"dayOfMonth\":20,\"startsOn\":\"2026-10-01\"}")
            .formatted(categoryId, creditCardId == null ? "null" : "\"" + creditCardId + "\"");
    }

    private static String recurringIncomeJson(String categoryId) {
        return "{\"categoryId\":\"%s\",\"description\":\"Salário\",\"amount\":5000,\"dayOfMonth\":25,\"startsOn\":\"2026-10-01\"}"
            .formatted(categoryId);
    }

    private static String planJson(String creditCardId, String categoryId) {
        return ("{\"creditCardId\":\"%s\",\"categoryId\":\"%s\",\"description\":\"Notebook\",\"totalAmount\":3000,"
            + "\"installmentsCount\":3,\"purchasedOn\":\"2026-10-13\"}")
            .formatted(creditCardId, categoryId);
    }
}
