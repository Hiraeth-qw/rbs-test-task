package ru.rbs.test.task.exception;

public class TicketConflictException extends RuntimeException {
    public TicketConflictException(String message) {
        super(message);
    }
}
