package ru.rbs.test.task.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rbs.test.task.dto.TakeTicketRequest;
import ru.rbs.test.task.dto.TicketDto;
import ru.rbs.test.task.enumeration.ServiceType;
import ru.rbs.test.task.enumeration.Status;
import ru.rbs.test.task.service.TicketService;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto takeTicket(@RequestBody @Valid TakeTicketRequest request) {
        return ticketService.createTicket(request);
    }

    @GetMapping("/{id}")
    public TicketDto getTicket(@PathVariable Long id) {
        return ticketService.getTicketById(id);
    }

    @GetMapping
    public List<TicketDto> getAllTickets(@RequestParam(required = false) Status status,
                                         @RequestParam(required = false) ServiceType serviceType) {
        return ticketService.getTicketList(status, serviceType);
    }

    @PatchMapping("/next")
    @ResponseStatus(HttpStatus.OK)
    public TicketDto callNextTicket(@RequestParam(required = false) ServiceType serviceType) {
        return ticketService.callNext(serviceType);
    }

    @PatchMapping("/{id}/done")
    @ResponseStatus(HttpStatus.OK)
    public void doneTicket(@PathVariable Long id) throws Exception {
        ticketService.doneTicket(id);
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public void cancelTicket(@PathVariable Long id) throws Exception {
        ticketService.cancelTicket(id);
    }
}
