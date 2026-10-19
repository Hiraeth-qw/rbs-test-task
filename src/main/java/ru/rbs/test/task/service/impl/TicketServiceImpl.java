package ru.rbs.test.task.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rbs.test.task.dto.TakeTicketRequest;
import ru.rbs.test.task.dto.TicketDto;
import ru.rbs.test.task.enumeration.ServiceType;
import ru.rbs.test.task.enumeration.Status;
import ru.rbs.test.task.model.Ticket;
import ru.rbs.test.task.repository.TicketRepository;
import ru.rbs.test.task.service.TicketService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public TicketDto createTicket(TakeTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setClientName(request.clientName());
        ticket.setService(request.service());

        Integer num = ticketRepository.findMaxNumber().orElse(0) + 1;
        ticket.setNumber(num);

        return toDto(ticketRepository.save(ticket));
    }

    @Override
    public TicketDto getTicketById(Long id) {
        Ticket ticket = findByIdOrThrow(id); //404, сделать обработчик ошибок и свои исключения
        return toDto(ticket);
    }

    @Override
    public List<TicketDto> getTicketList(Status status, ServiceType serviceType) {
        Specification<Ticket> spec = ((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (serviceType != null) {
                predicates.add(cb.equal(root.get("service"), serviceType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        return ticketRepository.findAll(spec, Sort.by("createdAt")).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TicketDto callNext(ServiceType serviceType) {
        // разобраться с логикой вызова следующего талона
        // если по всем услугам уже SERVING, то можно ли вызывать следующий WAITING?
        // при next талон не завершается автоматически, значит по идее можно вызвать еще один талон по той же услуге?
        // а нет, нельзя же два SERVING, значит при отсутствии свободной услуги просто исключение
        return null;
    }

    @Override
    @Transactional
    public void doneTicket(Long id) throws Exception {
        Ticket ticket = findByIdOrThrow(id);

        if (ticket.getStatus() != Status.SERVING) throw new Exception(); // 409
        ticket.setStatus(Status.DONE);
    }

    @Override
    @Transactional
    public void cancelTicket(Long id) throws Exception {
        Ticket ticket = findByIdOrThrow(id);

        if (ticket.getStatus() != Status.WAITING) throw new Exception(); // 409
        ticket.setStatus(Status.CANCELLED);
    }

    private TicketDto toDto(Ticket ticket) {
        return new TicketDto(ticket.getId(),
                ticket.getNumber(),
                ticket.getClientName(),
                ticket.getService(),
                ticket.getStatus());
    }

    private Ticket findByIdOrThrow(Long id) {
        return ticketRepository.findById(id).orElseThrow(); // 404, сделать обработчик ошибок и свои исключения
    }
}
