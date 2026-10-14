package com.penny.service;

import com.penny.dto.MeRequest;
import com.penny.dto.UserResponse;
import com.penny.exception.BusinessRuleException;
import com.penny.model.ExpenseCategory;
import com.penny.model.IncomeCategory;
import com.penny.model.User;
import com.penny.repository.ExpenseCategoryRepository;
import com.penny.repository.IncomeCategoryRepository;
import com.penny.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    public record Registration(UserResponse user, boolean created) {
    }

    private final UserRepository users;
    private final ExpenseCategoryRepository expenseCategories;
    private final IncomeCategoryRepository incomeCategories;

    public UserService(
        UserRepository users,
        ExpenseCategoryRepository expenseCategories,
        IncomeCategoryRepository incomeCategories
    ) {
        this.users = users;
        this.expenseCategories = expenseCategories;
        this.incomeCategories = incomeCategories;
    }

    @Transactional
    public Registration register(long telegramUserId, MeRequest request) {
        String name = blankToNull(request.name());
        String timezone = validTimezone(request.timezone());

        Optional<User> existing = users.findByTelegramUserId(telegramUserId);
        if (existing.isPresent()) {
            User user = existing.get();
            if (name != null) {
                user.setName(name);
            }
            if (timezone != null) {
                user.setTimezone(timezone);
            }
            return new Registration(UserResponse.from(user), false);
        }

        User user = users.saveAndFlush(new User(
            telegramUserId,
            name,
            timezone == null ? User.DEFAULT_TIMEZONE : timezone
        ));
        createDefaultCategories(user);
        return new Registration(UserResponse.from(user), true);
    }

    private void createDefaultCategories(User user) {
        List<ExpenseCategory> expenses = new ArrayList<>();
        DefaultCategories.EXPENSE.forEach(name -> expenses.add(new ExpenseCategory(user, name, false)));
        expenses.add(new ExpenseCategory(user, DefaultCategories.EXPENSE_FALLBACK, true));
        expenseCategories.saveAll(expenses);
        DefaultCategories.INCOME.forEach(name -> incomeCategories.save(new IncomeCategory(user, name)));
    }

    private String validTimezone(String timezone) {
        String value = blankToNull(timezone);
        if (value == null) {
            return null;
        }
        try {
            return ZoneId.of(value).getId();
        } catch (DateTimeException e) {
            throw new BusinessRuleException("INVALID_TIMEZONE", "Fuso horário inválido: " + value + ".");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
