package ru.rbs.test.task.service;

import ru.rbs.test.task.dto.TakeTicketRequest;
import ru.rbs.test.task.dto.TicketDto;
import ru.rbs.test.task.enumeration.ServiceType;
import ru.rbs.test.task.enumeration.Status;

import java.util.List;

public interface TicketService {
    TicketDto createTicket(TakeTicketRequest request);
    TicketDto getTicketById(Long id);
    List<TicketDto> getTicketList(Status status, ServiceType serviceType);
    TicketDto callNext(ServiceType serviceType);
    void doneTicket(Long id);
    void cancelTicket(Long id);
}
