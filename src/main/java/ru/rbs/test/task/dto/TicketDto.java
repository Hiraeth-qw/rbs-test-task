package ru.rbs.test.task.dto;

import ru.rbs.test.task.enumeration.ServiceType;
import ru.rbs.test.task.enumeration.Status;

public record TicketDto(Long id, Integer number, String clientName,
                        ServiceType service, Status status) {
}
