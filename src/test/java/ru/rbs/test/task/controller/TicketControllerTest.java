package ru.rbs.test.task.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rbs.test.task.dto.TicketDto;
import ru.rbs.test.task.enumeration.ServiceType;
import ru.rbs.test.task.enumeration.Status;
import ru.rbs.test.task.exception.TicketConflictException;
import ru.rbs.test.task.exception.TicketNotFoundException;
import ru.rbs.test.task.service.TicketService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void takeTicket_validRequest_returns201() throws Exception {
        when(ticketService.createTicket(any())).thenReturn(dto(1L, Status.WAITING));

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientName": "Иван", "service": "PAYMENTS"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void takeTicket_blankName_returns400WithErrorBody() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientName": "  ", "service": "PAYMENTS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Ошибка валидации"))
                .andExpect(jsonPath("$.details").isNotEmpty());

        verify(ticketService, never()).createTicket(any());
    }

    @Test
    void takeTicket_nameLongerThan100_returns400() throws Exception {
        String longName = "а".repeat(101);

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientName\": \"" + longName + "\", \"service\": \"PAYMENTS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void takeTicket_missingService_returns400() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientName": "Иван"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void takeTicket_unknownServiceValue_returns400() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientName": "Иван", "service": "SixSeven"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getTicket_found_returns200() throws Exception {
        when(ticketService.getTicketById(1L)).thenReturn(dto(1L, Status.WAITING));

        mockMvc.perform(get("/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clientName").value("Иван"));
    }

    @Test
    void getTicket_notFound_returns404() throws Exception {
        when(ticketService.getTicketById(99L))
                .thenThrow(new TicketNotFoundException("Талон с id: 99 не найден."));

        mockMvc.perform(get("/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Талон с id: 99 не найден."));
    }

    @Test
    void getAllTickets_passesFiltersStatusAndService() throws Exception {
        when(ticketService.getTicketList(Status.WAITING, ServiceType.PAYMENTS))
                .thenReturn(List.of(dto(1L, Status.WAITING)));

        mockMvc.perform(get("/tickets")
                        .param("status", "WAITING")
                        .param("service", "PAYMENTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(ticketService).getTicketList(Status.WAITING, ServiceType.PAYMENTS);
    }

    @Test
    void getAllTickets_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/tickets").param("status", "MOGGED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void callNext_withServiceParam_returns200() throws Exception {
        when(ticketService.callNext(ServiceType.PAYMENTS)).thenReturn(dto(1L, Status.SERVING));

        mockMvc.perform(patch("/tickets/next").param("service", "PAYMENTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SERVING"))
                .andExpect(jsonPath("$.calledAt").isNotEmpty());

        verify(ticketService).callNext(ServiceType.PAYMENTS);
    }

    @Test
    void callNext_withoutParam_passesNull() throws Exception {
        when(ticketService.callNext(null)).thenReturn(dto(1L, Status.SERVING));

        mockMvc.perform(patch("/tickets/next"))
                .andExpect(status().isOk());

        verify(ticketService).callNext(null);
    }

    @Test
    void callNext_nothingToCall_returns404() throws Exception {
        when(ticketService.callNext(null))
                .thenThrow(new TicketNotFoundException("Нет ожидающих талонов."));

        mockMvc.perform(patch("/tickets/next"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void callNext_serviceBusy_returns409() throws Exception {
        when(ticketService.callNext(ServiceType.PAYMENTS))
                .thenThrow(new TicketConflictException("Талон для услуги PAYMENTS уже обслуживается."));

        mockMvc.perform(patch("/tickets/next").param("service", "PAYMENTS"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void doneTicket_ok_returns200() throws Exception {
        mockMvc.perform(patch("/tickets/1/done"))
                .andExpect(status().isOk());

        verify(ticketService).doneTicket(1L);
    }

    @Test
    void doneTicket_wrongStatus_returns409() throws Exception {
        doThrow(new TicketConflictException("Невозможно завершить"))
                .when(ticketService).doneTicket(1L);

        mockMvc.perform(patch("/tickets/1/done"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void cancelTicket_notFound_returns404() throws Exception {
        doThrow(new TicketNotFoundException("Талон не найден"))
                .when(ticketService).cancelTicket(5L);

        mockMvc.perform(patch("/tickets/5/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void cancelTicket_wrongStatus_returns409() throws Exception {
        doThrow(new TicketConflictException("Невозможно отменить"))
                .when(ticketService).cancelTicket(1L);

        mockMvc.perform(patch("/tickets/1/cancel"))
                .andExpect(status().isConflict());
    }

    private TicketDto dto(Long id, Status status) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 12, 0);
        return new TicketDto(id, id.intValue(), "Иван", ServiceType.PAYMENTS, status, now,
                status == Status.SERVING ? now.plusMinutes(5) : null);
    }
}