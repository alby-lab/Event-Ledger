package com.eventledger.account.entity;

import com.eventledger.account.util.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_event_id", columnList = "eventId", unique = true),
        @Index(name = "idx_tx_account_id", columnList = "accountId")
})
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant eventTimestamp;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public TransactionEntity() {}

    private TransactionEntity(Builder b) {
        this.id = b.id;
        this.eventId = b.eventId;
        this.accountId = b.accountId;
        this.type = b.type;
        this.amount = b.amount;
        this.eventTimestamp = b.eventTimestamp;
        this.createdAt = b.createdAt;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String eventId, accountId;
        private TransactionType type;
        private BigDecimal amount;
        private Instant eventTimestamp, createdAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder eventId(String v) { this.eventId = v; return this; }
        public Builder accountId(String v) { this.accountId = v; return this; }
        public Builder type(TransactionType v) { this.type = v; return this; }
        public Builder amount(BigDecimal v) { this.amount = v; return this; }
        public Builder eventTimestamp(Instant v) { this.eventTimestamp = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public TransactionEntity build() { return new TransactionEntity(this); }
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getAccountId() { return accountId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long v) { this.id = v; }
    public void setEventId(String v) { this.eventId = v; }
    public void setAccountId(String v) { this.accountId = v; }
    public void setType(TransactionType v) { this.type = v; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public void setEventTimestamp(Instant v) { this.eventTimestamp = v; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}
