package com.eventledger.gateway.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.eventledger.gateway.dto.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "events", indexes = { @Index(name = "idx_event_id", columnList = "eventId", unique = true),
		@Index(name = "idx_account_id", columnList = "accountId") })
public class EventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String eventId;

	@Column(nullable = false)
	private String accountId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventType type;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(nullable = false, length = 10)
	private String currency;

	@Column(nullable = false)
	private Instant eventTimestamp;


	@Column(columnDefinition = "TEXT")
	private Map<String, String> metadata;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	public EventEntity() {
	}

	private EventEntity(Builder b) {
		this.id = b.id;
		this.eventId = b.eventId;
		this.accountId = b.accountId;
		this.type = b.type;
		this.amount = b.amount;
		this.currency = b.currency;
		this.eventTimestamp = b.eventTimestamp;
		this.metadata = b.metadata;
		this.createdAt = b.createdAt;
	}

	@PrePersist
	public void prePersist() {
		createdAt = Instant.now();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Long id;
		private String eventId, accountId, currency;
		private EventType type;
		private BigDecimal amount;
		private Instant eventTimestamp, createdAt;
		private Map<String, String> metadata;

		public Builder id(Long id) {
			this.id = id;
			return this;
		}

		public Builder eventId(String v) {
			this.eventId = v;
			return this;
		}

		public Builder accountId(String v) {
			this.accountId = v;
			return this;
		}

		public Builder type(EventType v) {
			this.type = v;
			return this;
		}

		public Builder amount(BigDecimal v) {
			this.amount = v;
			return this;
		}

		public Builder currency(String v) {
			this.currency = v;
			return this;
		}

		public Builder eventTimestamp(Instant v) {
			this.eventTimestamp = v;
			return this;
		}

		public Builder metadata(Map<String, String> v) {
			this.metadata = v;
			return this;
		}

		public Builder createdAt(Instant v) {
			this.createdAt = v;
			return this;
		}

		public EventEntity build() {
			return new EventEntity(this);
		}
	}

	public Long getId() {
		return id;
	}

	public String getEventId() {
		return eventId;
	}

	public String getAccountId() {
		return accountId;
	}

	public EventType getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public Instant getEventTimestamp() {
		return eventTimestamp;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setEventId(String v) {
		this.eventId = v;
	}

	public void setAccountId(String v) {
		this.accountId = v;
	}

	public void setType(EventType v) {
		this.type = v;
	}

	public void setAmount(BigDecimal v) {
		this.amount = v;
	}

	public void setCurrency(String v) {
		this.currency = v;
	}

	public void setEventTimestamp(Instant v) {
		this.eventTimestamp = v;
	}

	public void setMetadata(Map<String, String> v) {
		this.metadata = v;
	}

	public void setCreatedAt(Instant v) {
		this.createdAt = v;
	}

}
