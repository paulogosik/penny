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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    private String name;

    private String color;

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyBudget;

    @Column(name = "is_fallback")
    private boolean fallback;

    private Instant archivedAt;

    @CreationTimestamp
    private Instant createdAt;

    protected ExpenseCategory() {
    }

    public ExpenseCategory(User user, String name, boolean fallback) {
        this.user = user;
        this.name = name;
        this.fallback = fallback;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public void archive(Instant when) {
        this.archivedAt = when;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public boolean isFallback() {
        return fallback;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
