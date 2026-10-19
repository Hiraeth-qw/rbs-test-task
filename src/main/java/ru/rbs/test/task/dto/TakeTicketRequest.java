package ru.rbs.test.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.rbs.test.task.enumeration.ServiceType;

public record TakeTicketRequest(@NotBlank @Size(max = 100) String clientName,
                                @NotNull ServiceType service) {
}
