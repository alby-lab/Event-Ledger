package com.eventledger.account.mapper;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.entity.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(AccountEntity entity) {
        return new AccountResponse(
                entity.getAccountId(),
                entity.getCurrentBalance(),
                entity.getCurrency(),
                entity.getCreatedAt()
        );
    }

    public BalanceResponse toBalanceResponse(AccountEntity entity) {
        return new BalanceResponse(
                entity.getAccountId(),
                entity.getCurrentBalance(),
                entity.getCurrency()
        );
    }
}
