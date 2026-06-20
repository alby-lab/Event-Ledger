package com.eventledger.account.mapper;

import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.entity.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(TransactionEntity entity) {
        return new TransactionResponse(
                entity.getId(),
                entity.getEventId(),
                entity.getAccountId(),
                entity.getType().name(),
                entity.getAmount(),
                entity.getEventTimestamp(),
                entity.getCreatedAt()
        );
    }
}
