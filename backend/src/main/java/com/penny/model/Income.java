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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "incomes")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private IncomeCategory category;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    private String description;

    private LocalDate receivedOn;

    private IncomeSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    private RecurringIncome recurringIncome;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected Income() {
    }

    public Income(User user, IncomeSource source) {
        this.user = user;
        this.source = source;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public IncomeCategory getCategory() {
        return category;
    }

    public void setCategory(IncomeCategory category) {
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

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public void setReceivedOn(LocalDate receivedOn) {
        this.receivedOn = receivedOn;
    }

    public IncomeSource getSource() {
        return source;
    }

    public RecurringIncome getRecurringIncome() {
        return recurringIncome;
    }

    public void setRecurringIncome(RecurringIncome recurringIncome) {
        this.recurringIncome = recurringIncome;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
