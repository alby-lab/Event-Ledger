package com.eventledger.account.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_id", columnList = "accountId", unique = true)
})
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public AccountEntity() {}

    private AccountEntity(Builder b) {
        this.id = b.id;
        this.accountId = b.accountId;
        this.currentBalance = b.currentBalance;
        this.currency = b.currency;
        this.createdAt = b.createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (currentBalance == null) currentBalance = BigDecimal.ZERO;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String accountId, currency;
        private BigDecimal currentBalance;
        private Instant createdAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder accountId(String v) { this.accountId = v; return this; }
        public Builder currentBalance(BigDecimal v) { this.currentBalance = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public AccountEntity build() { return new AccountEntity(this); }
    }

    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long v) { this.id = v; }
    public void setAccountId(String v) { this.accountId = v; }
    public void setCurrentBalance(BigDecimal v) { this.currentBalance = v; }
    public void setCurrency(String v) { this.currency = v; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
