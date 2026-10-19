package com.penny.service;

import com.penny.dto.ExpenseFilter;
import com.penny.dto.ExpenseRequest;
import com.penny.dto.ExpenseResponse;
import com.penny.dto.PageResponse;
import com.penny.exception.BusinessRuleException;
import com.penny.exception.NotFoundException;
import com.penny.model.CreditCard;
import com.penny.model.Expense;
import com.penny.model.ExpenseCategory;
import com.penny.model.ExpenseSource;
import com.penny.model.User;
import com.penny.repository.CreditCardRepository;
import com.penny.repository.ExpenseCategoryRepository;
import com.penny.repository.ExpenseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.penny.repository.ExpenseSpecifications.belongsTo;
import static com.penny.repository.ExpenseSpecifications.billedIn;
import static com.penny.repository.ExpenseSpecifications.chargedTo;
import static com.penny.repository.ExpenseSpecifications.inCategory;
import static com.penny.repository.ExpenseSpecifications.occurredFrom;
import static com.penny.repository.ExpenseSpecifications.occurredUntil;

@Service
public class ExpenseService {

    private static final int MAX_INSTALLMENTS = 99;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private final ExpenseRepository expenses;
    private final ExpenseCategoryRepository categories;
    private final CreditCardRepository creditCards;
    private final InstallmentPlanService installmentPlans;
    private final ExpenseClassifier classifier;
    private final EntityLookup lookup;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ExpenseService(
        ExpenseRepository expenses,
        ExpenseCategoryRepository categories,
        CreditCardRepository creditCards,
        InstallmentPlanService installmentPlans,
        ExpenseClassifier classifier,
        EntityLookup lookup,
        PlatformTransactionManager transactionManager,
        Clock clock
    ) {
        this.expenses = expenses;
        this.categories = categories;
        this.creditCards = creditCards;
        this.installmentPlans = installmentPlans;
        this.classifier = classifier;
        this.lookup = lookup;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public ExpenseResponse recordFromText(User user, String text) {
        ensurePhoneNumber(user);
        ClassifiedExpense classified = classifier.classifyText(text.strip(), contextFor(user));
        return transactionTemplate.execute(status -> record(user, classified, ExpenseSource.TELEGRAM_TEXT, text.strip()));
    }

    public ExpenseResponse recordFromReceipt(User user, String imageBase64, String mimeType) {
        ensurePhoneNumber(user);
        byte[] image = decodeImage(imageBase64);
        ClassifiedExpense classified = classifier.classifyReceipt(image, mimeType, contextFor(user));
        return transactionTemplate.execute(status -> record(user, classified, ExpenseSource.TELEGRAM_PHOTO, null));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(User user, ExpenseFilter filter, int page, int size) {
        List<Specification<Expense>> specifications = Stream.of(
            belongsTo(user),
            occurredFrom(filter.from()),
            occurredUntil(filter.to()),
            inCategory(filter.categoryId()),
            chargedTo(filter.creditCardId()),
            billedIn(filter.billingMonth() == null ? null : filter.billingMonth().atDay(1))
        ).filter(Objects::nonNull).toList();
        PageRequest pageRequest = PageRequest.of(
            Math.max(0, page),
            Math.clamp(size, 1, 100),
            Sort.by(Sort.Order.desc("occurredOn"), Sort.Order.desc("createdAt"))
        );
        return PageResponse.of(expenses.findAll(Specification.allOf(specifications), pageRequest).map(ExpenseResponse::from));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(User user, UUID id) {
        return ExpenseResponse.from(find(user, id));
    }

    @Transactional
    public ExpenseResponse create(User user, ExpenseRequest request) {
        Expense expense = new Expense(user, ExpenseSource.PANEL);
        apply(user, expense, request);
        return ExpenseResponse.from(expenses.saveAndFlush(expense));
    }

    @Transactional
    public ExpenseResponse update(User user, UUID id, ExpenseRequest request) {
        Expense expense = find(user, id);
        ensureNotInstallment(expense);
        apply(user, expense, request);
        return ExpenseResponse.from(expenses.saveAndFlush(expense));
    }

    @Transactional
    public void delete(User user, UUID id) {
        Expense expense = find(user, id);
        ensureNotInstallment(expense);
        expenses.delete(expense);
    }

    private ExpenseResponse record(User user, ClassifiedExpense classified, ExpenseSource source, String rawInput) {
        if (classified.amount() == null || classified.amount().signum() <= 0) {
            throw new BusinessRuleException(
                "AMOUNT_NOT_FOUND",
                "Não consegui identificar o valor do gasto. Tente algo como \"gastei 45 no mercado\"."
            );
        }
        ExpenseCategory category = resolveCategory(user, classified.category());
        CreditCard card = resolveCreditCard(user, classified.creditCard());
        String description = describe(classified.description(), category);

        if (classified.installments() > 1) {
            if (card == null) {
                throw new BusinessRuleException(
                    "INSTALLMENTS_REQUIRE_CREDIT_CARD",
                    "Compras parceladas precisam de um cartão cadastrado. Cadastre o cartão no painel e diga qual foi usado, ex.: \"em 6x no Nubank\"."
                );
            }
            if (classified.installments() > MAX_INSTALLMENTS) {
                throw new BusinessRuleException("INVALID_INSTALLMENTS", "O número máximo de parcelas é " + MAX_INSTALLMENTS + ".");
            }
            List<Expense> installments = installmentPlans.createPlan(
                user, card, category, description, classified.amount(), classified.installments(),
                classified.date(), source, rawInput, classified.model()
            );
            return ExpenseResponse.from(installments.getFirst());
        }

        Expense expense = new Expense(user, source);
        expense.setCategory(category);
        expense.setAmount(classified.amount());
        expense.setDescription(description);
        expense.setOccurredOn(classified.date());
        expense.chargeTo(card, card == null ? null : CreditCardBilling.billingMonth(card, classified.date()));
        expense.setRawInput(rawInput);
        expense.setAiModel(classified.model());
        return ExpenseResponse.from(expenses.saveAndFlush(expense));
    }

    private ClassificationContext contextFor(User user) {
        List<ExpenseCategory> active = categories.findByUserAndArchivedAtIsNullOrderByName(user);
        String fallback = active.stream()
            .filter(ExpenseCategory::isFallback)
            .map(ExpenseCategory::getName)
            .findFirst()
            .orElse(DefaultCategories.EXPENSE_FALLBACK);
        List<String> cards = creditCards.findByUserAndArchivedAtIsNullOrderByName(user)
            .stream()
            .map(CreditCard::getName)
            .toList();
        return new ClassificationContext(
            active.stream().map(ExpenseCategory::getName).toList(),
            fallback,
            cards,
            LocalDate.now(clock.withZone(user.zoneId()))
        );
    }

    private ExpenseCategory resolveCategory(User user, String name) {
        return categories.findByUserAndArchivedAtIsNullOrderByName(user).stream()
            .filter(category -> normalize(category.getName()).equals(normalize(name)))
            .findFirst()
            .orElseGet(() -> categories.findByUserAndFallbackTrue(user)
                .orElseThrow(() -> new IllegalStateException("Usuário sem categoria de reserva: " + user.getId())));
    }

    private CreditCard resolveCreditCard(User user, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String wanted = normalize(name);
        List<CreditCard> active = creditCards.findByUserAndArchivedAtIsNullOrderByName(user);
        return active.stream()
            .filter(card -> normalize(card.getName()).equals(wanted))
            .findFirst()
            .or(() -> active.stream().filter(card -> wanted.contains(normalize(card.getName()))).findFirst())
            .orElse(null);
    }

    private void apply(User user, Expense expense, ExpenseRequest request) {
        CreditCard card = lookup.optionalActiveCreditCard(user, request.creditCardId());
        expense.setCategory(lookup.activeExpenseCategory(user, request.categoryId()));
        expense.setAmount(request.amount());
        expense.setDescription(request.description().strip());
        expense.setOccurredOn(request.occurredOn());
        expense.chargeTo(card, card == null ? null : CreditCardBilling.billingMonth(card, request.occurredOn()));
    }

    private Expense find(User user, UUID id) {
        return expenses.findByIdAndUser(id, user)
            .orElseThrow(() -> new NotFoundException("EXPENSE_NOT_FOUND", "Gasto não encontrado."));
    }

    private void ensurePhoneNumber(User user) {
        if (!user.hasPhoneNumber()) {
            throw new BusinessRuleException(
                "PHONE_NUMBER_REQUIRED",
                "Compartilhe seu número de telefone para começar a registrar gastos."
            );
        }
    }

    private void ensureNotInstallment(Expense expense) {
        if (expense.isInstallment()) {
            throw new BusinessRuleException(
                "INSTALLMENT_MANAGED_BY_PLAN",
                "Esse gasto é uma parcela. Altere ou remova o parcelamento inteiro."
            );
        }
    }

    private byte[] decodeImage(String imageBase64) {
        byte[] image;
        try {
            image = Base64.getMimeDecoder().decode(imageBase64);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_IMAGE", "A imagem enviada não está em base64 válido.");
        }
        if (image.length == 0 || image.length > MAX_IMAGE_BYTES) {
            throw new BusinessRuleException("INVALID_IMAGE", "A imagem precisa ter entre 1 byte e 10 MB.");
        }
        return image;
    }

    private static String describe(String description, ExpenseCategory category) {
        String value = description == null || description.isBlank() ? category.getName() : description.strip();
        return value.length() <= MAX_DESCRIPTION_LENGTH ? value : value.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.strip().toLowerCase(Locale.ROOT);
    }
}
