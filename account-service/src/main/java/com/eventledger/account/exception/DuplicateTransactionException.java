package com.eventledger.account.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String eventId) {
        super("Transaction already processed for eventId: " + eventId);
    }
}
