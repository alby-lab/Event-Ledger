package com.eventledger.gateway.mapper;

import com.eventledger.gateway.dto.AccountTransactionRequest;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.entity.EventEntity;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventEntity toEntity(EventRequest request) {
        return EventEntity.builder()
                .eventId(request.eventId())
                .accountId(request.accountId())
                .type(request.type())
                .amount(request.amount())
                .currency(request.currency())
                .eventTimestamp(request.eventTimestamp())
                .metadata(request.metadata())
                .build();
    }

    public EventResponse toResponse(EventEntity entity) {
        return new EventResponse(
                entity.getEventId(),
                entity.getAccountId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getEventTimestamp(),
                entity.getMetadata(),
                entity.getCreatedAt()
        );
    }

    public AccountTransactionRequest toTransactionRequest(EventRequest request) {
        return new AccountTransactionRequest(
                request.eventId(),
                request.accountId(),
                request.type().name(),
                request.amount(),
                request.eventTimestamp(),
                request.currency()
        );
    }
}
