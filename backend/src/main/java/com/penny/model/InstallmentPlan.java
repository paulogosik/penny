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
@Table(name = "installment_plans")
public class InstallmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CreditCard creditCard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ExpenseCategory category;

    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer installmentsCount;

    private LocalDate purchasedOn;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected InstallmentPlan() {
    }

    public InstallmentPlan(
        User user,
        CreditCard creditCard,
        ExpenseCategory category,
        String description,
        BigDecimal totalAmount,
        Integer installmentsCount,
        LocalDate purchasedOn
    ) {
        this.user = user;
        this.creditCard = creditCard;
        this.category = category;
        this.description = description;
        this.totalAmount = totalAmount;
        this.installmentsCount = installmentsCount;
        this.purchasedOn = purchasedOn;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Integer getInstallmentsCount() {
        return installmentsCount;
    }

    public LocalDate getPurchasedOn() {
        return purchasedOn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
