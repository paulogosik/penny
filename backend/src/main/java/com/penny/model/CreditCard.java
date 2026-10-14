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
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_cards")
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    private String name;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer closingDay;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer dueDay;

    @Column(precision = 12, scale = 2)
    private BigDecimal creditLimit;

    private Instant archivedAt;

    @CreationTimestamp
    private Instant createdAt;

    protected CreditCard() {
    }

    public CreditCard(User user, String name, Integer closingDay, Integer dueDay) {
        this.user = user;
        this.name = name;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
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

    public Integer getClosingDay() {
        return closingDay;
    }

    public void setClosingDay(Integer closingDay) {
        this.closingDay = closingDay;
    }

    public Integer getDueDay() {
        return dueDay;
    }

    public void setDueDay(Integer dueDay) {
        this.dueDay = dueDay;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
