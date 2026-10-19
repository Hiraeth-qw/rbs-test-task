package ru.rbs.test.task.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rbs.test.task.dto.TakeTicketRequest;
import ru.rbs.test.task.dto.TicketDto;
import ru.rbs.test.task.enumeration.ServiceType;
import ru.rbs.test.task.enumeration.Status;
import ru.rbs.test.task.exception.TicketConflictException;
import ru.rbs.test.task.exception.TicketNotFoundException;
import ru.rbs.test.task.model.Ticket;
import ru.rbs.test.task.repository.TicketRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    void createTicket_assignsNextNumberAndWaitingStatus() {
        when(ticketRepository.findMaxNumber()).thenReturn(Optional.of(5));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketDto result = ticketService.createTicket(new TakeTicketRequest("Иван", ServiceType.PAYMENTS));

        assertThat(result.number()).isEqualTo(6);
        assertThat(result.status()).isEqualTo(Status.WAITING);
        assertThat(result.clientName()).isEqualTo("Иван");
        assertThat(result.service()).isEqualTo(ServiceType.PAYMENTS);
        assertThat(result.calledAt()).isNull();
    }

    @Test
    void createTicket_firstTicketGetsNumberOne() {
        when(ticketRepository.findMaxNumber()).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketDto result = ticketService.createTicket(new TakeTicketRequest("Анна", ServiceType.DOCUMENTS));

        assertThat(result.number()).isEqualTo(1);
    }

    @Test
    void getTicketById_notFound_throwsNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void callNext_withService_setsServingAndCalledAt() {
        Ticket ticket = ticket(1L, ServiceType.PAYMENTS, Status.WAITING);
        when(ticketRepository.existsByServiceAndStatus(ServiceType.PAYMENTS, Status.SERVING))
                .thenReturn(false);
        when(ticketRepository.findFirstByServiceAndStatusOrderByCreatedAtAscIdAsc(
                ServiceType.PAYMENTS, Status.WAITING)).thenReturn(Optional.of(ticket));

        TicketDto result = ticketService.callNext(ServiceType.PAYMENTS);

        assertThat(result.status()).isEqualTo(Status.SERVING);
        assertThat(result.calledAt()).isNotNull();
        assertThat(ticket.getStatus()).isEqualTo(Status.SERVING);
    }

    @Test
    void callNext_withService_serviceAlreadyServing_throwsConflict() {
        when(ticketRepository.existsByServiceAndStatus(ServiceType.PAYMENTS, Status.SERVING))
                .thenReturn(true);

        assertThatThrownBy(() -> ticketService.callNext(ServiceType.PAYMENTS))
                .isInstanceOf(TicketConflictException.class);

        verify(ticketRepository, never())
                .findFirstByServiceAndStatusOrderByCreatedAtAscIdAsc(any(), any());
    }

    @Test
    void callNext_withService_noWaitingTickets_throwsNotFound() {
        when(ticketRepository.existsByServiceAndStatus(ServiceType.DOCUMENTS, Status.SERVING))
                .thenReturn(false);
        when(ticketRepository.findFirstByServiceAndStatusOrderByCreatedAtAscIdAsc(
                ServiceType.DOCUMENTS, Status.WAITING)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.callNext(ServiceType.DOCUMENTS))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void callNext_withoutService_takesTicketFromFreeService() {
        Ticket ticket = ticket(2L, ServiceType.DOCUMENTS, Status.WAITING);

        when(ticketRepository.findNextAmongFreeServices(Status.WAITING,
                Status.SERVING))
                .thenReturn(Optional.of(ticket));

        TicketDto result = ticketService.callNext(null);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.status()).isEqualTo(Status.SERVING);
        assertThat(result.calledAt()).isNotNull();
    }

    @Test
    void callNext_withoutService_nothingAvailable_throwsNotFound() {
        when(ticketRepository.findNextAmongFreeServices(
                Status.WAITING, Status.SERVING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.callNext(null))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void doneTicket_serving_becomesDone() {
        Ticket ticket = ticket(1L, ServiceType.PAYMENTS, Status.SERVING);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        ticketService.doneTicket(1L);

        assertThat(ticket.getStatus()).isEqualTo(Status.DONE);
    }

    @Test
    void doneTicket_waiting_throwsConflict() {
        Ticket ticket = ticket(1L, ServiceType.PAYMENTS, Status.WAITING);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.doneTicket(1L))
                .isInstanceOf(TicketConflictException.class);

        assertThat(ticket.getStatus()).isEqualTo(Status.WAITING);
    }

    @Test
    void cancelTicket_waiting_becomesCancelled() {
        Ticket ticket = ticket(1L, ServiceType.PAYMENTS, Status.WAITING);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        ticketService.cancelTicket(1L);

        assertThat(ticket.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void cancelTicket_serving_throwsConflict() {
        Ticket ticket = ticket(1L, ServiceType.PAYMENTS, Status.SERVING);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.cancelTicket(1L))
                .isInstanceOf(TicketConflictException.class);

        assertThat(ticket.getStatus()).isEqualTo(Status.SERVING);
    }

    @Test
    void cancelTicket_notFound_throwsNotFound() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.cancelTicket(42L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    private Ticket ticket(Long id, ServiceType service, Status status) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setNumber(id.intValue());
        t.setClientName("Клиент");
        t.setService(service);
        t.setStatus(status);
        return t;
    }
}