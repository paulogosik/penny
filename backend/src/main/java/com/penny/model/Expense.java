package com.penny.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ExpenseCategory category;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    private String description;

    private LocalDate occurredOn;

    private ExpenseSource source;

    private String rawInput;

    private String aiModel;

    private String receiptPath;

    @ManyToOne(fetch = FetchType.LAZY)
    private CreditCard creditCard;

    private LocalDate billingMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    private InstallmentPlan installmentPlan;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer installmentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    private RecurringExpense recurringExpense;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected Expense() {
    }

    public Expense(User user, ExpenseSource source) {
        this.user = user;
        this.source = source;
    }

    public boolean isInstallment() {
        return installmentPlan != null;
    }

    public void chargeTo(CreditCard creditCard, LocalDate billingMonth) {
        this.creditCard = creditCard;
        this.billingMonth = creditCard == null ? null : billingMonth;
    }

    public void linkInstallment(InstallmentPlan installmentPlan, Integer installmentNumber) {
        this.installmentPlan = installmentPlan;
        this.installmentNumber = installmentNumber;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(LocalDate occurredOn) {
        this.occurredOn = occurredOn;
    }

    public ExpenseSource getSource() {
        return source;
    }

    public String getRawInput() {
        return rawInput;
    }

    public void setRawInput(String rawInput) {
        this.rawInput = rawInput;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public LocalDate getBillingMonth() {
        return billingMonth;
    }

    public InstallmentPlan getInstallmentPlan() {
        return installmentPlan;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public RecurringExpense getRecurringExpense() {
        return recurringExpense;
    }

    public void setRecurringExpense(RecurringExpense recurringExpense) {
        this.recurringExpense = recurringExpense;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
