package ru.rbs.test.task.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rbs.test.task.dto.TakeTicketRequest;
import ru.rbs.test.task.dto.TicketDto;
import ru.rbs.test.task.enumeration.ServiceType;
import ru.rbs.test.task.enumeration.Status;
import ru.rbs.test.task.exception.TicketConflictException;
import ru.rbs.test.task.exception.TicketNotFoundException;
import ru.rbs.test.task.model.Ticket;
import ru.rbs.test.task.repository.TicketRepository;
import ru.rbs.test.task.service.TicketService;

import java.time.LocalDateTime;
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
        Ticket ticket = findByIdOrThrow(id);
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
        Ticket ticket;
        if (serviceType != null) {
            if (ticketRepository.existsByServiceAndStatus(serviceType, Status.SERVING))
                throw new TicketConflictException("Талон для услуги " + serviceType + " уже обслуживается.");
            ticket = ticketRepository.findFirstByServiceAndStatusOrderByCreatedAtAscIdAsc(serviceType, Status.WAITING)
                    .orElseThrow(() -> new TicketNotFoundException("Для услуги " + serviceType + " нет талонов в ожидании."));
        } else {
            ticket = ticketRepository.findNextAmongFreeServices(Status.WAITING, Status.SERVING, PageRequest.of(0, 1))
                    .stream().findFirst()
                    .orElseThrow(() -> new TicketNotFoundException("Нет ожидающих талонов."));
        }
        ticket.setStatus(Status.SERVING);
        ticket.setCalledAt(LocalDateTime.now());

        return toDto(ticket);
    }

    @Override
    @Transactional
    public void doneTicket(Long id) {
        Ticket ticket = findByIdOrThrow(id);

        if (ticket.getStatus() != Status.SERVING)
            throw new TicketConflictException("Невозможно завершить обслуживание талона со статусом: " + ticket.getStatus());
        ticket.setStatus(Status.DONE);
    }

    @Override
    @Transactional
    public void cancelTicket(Long id) {
        Ticket ticket = findByIdOrThrow(id);

        if (ticket.getStatus() != Status.WAITING)
            throw new TicketConflictException("Невозможно отменить талон со статусом: " + ticket.getStatus());
        ticket.setStatus(Status.CANCELLED);
    }

    private TicketDto toDto(Ticket ticket) {
        return new TicketDto(ticket.getId(),
                ticket.getNumber(),
                ticket.getClientName(),
                ticket.getService(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getCalledAt());
    }

    private Ticket findByIdOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Талон с id: " + id + " не найден."));
    }
}
